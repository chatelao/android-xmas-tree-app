package com.xmastree.remote.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.xmastree.remote.protocol.AudioControlFrame
import com.xmastree.remote.protocol.GattProfile
import com.xmastree.remote.protocol.LightingControlFrame
import com.xmastree.remote.protocol.TelemetryFrame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class AndroidBleClient(
    private val context: Context
) : BleClient {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices.asStateFlow()

    private val _telemetry = MutableStateFlow<TelemetryFrame?>(null)
    override val telemetry: StateFlow<TelemetryFrame?> = _telemetry.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    override val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val gattMutex = Mutex()
    private var activeGatt: BluetoothGatt? = null
    private var lightingChar: BluetoothGattCharacteristic? = null
    private var audioChar: BluetoothGattCharacteristic? = null
    private var telemetryChar: BluetoothGattCharacteristic? = null

    @Volatile
    private var pendingConnect: CompletableDeferred<Boolean>? = null

    @Volatile
    private var pendingGattOp: CompletableDeferred<Boolean>? = null

    @SuppressLint("MissingPermission")
    override fun scanForDevices(): Flow<ScannedDevice> = callbackFlow {
        if (!hasScanPermission()) {
            _lastError.value = "Missing Bluetooth scan permission"
            _connectionState.value = BleConnectionState.ERROR
            close()
            return@callbackFlow
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            _lastError.value = "Bluetooth LE scanner unavailable"
            _connectionState.value = BleConnectionState.ERROR
            close()
            return@callbackFlow
        }

        _scannedDevices.value = emptyList()
        _connectionState.value = BleConnectionState.SCANNING
        _lastError.value = null

        val seen = linkedMapOf<String, ScannedDevice>()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val name = result.scanRecord?.deviceName ?: device.name ?: "Unknown BLE Device"
                val entry = ScannedDevice(
                    name = name,
                    address = device.address,
                    rssi = result.rssi
                )
                seen[device.address] = entry
                _scannedDevices.value = seen.values.toList()
                trySend(entry)
            }

            override fun onScanFailed(errorCode: Int) {
                _lastError.value = "BLE scan failed (code=$errorCode)"
                _connectionState.value = BleConnectionState.ERROR
            }
        }

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(GattProfile.SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(listOf(filter), settings, callback)

        awaitClose {
            try {
                scanner.stopScan(callback)
            } catch (_: SecurityException) {
            }
            if (_connectionState.value == BleConnectionState.SCANNING) {
                _connectionState.value = BleConnectionState.DISCONNECTED
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String): Boolean {
        if (!hasConnectPermission()) {
            _lastError.value = "Missing Bluetooth connect permission"
            _connectionState.value = BleConnectionState.ERROR
            return false
        }
        val adapter = bluetoothAdapter ?: run {
            _lastError.value = "Bluetooth adapter unavailable"
            _connectionState.value = BleConnectionState.ERROR
            return false
        }

        disconnect()
        _lastError.value = null

        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            _lastError.value = "Invalid BLE address: $address"
            _connectionState.value = BleConnectionState.ERROR
            return false
        }

        repeat(MAX_CONNECT_RETRIES + 1) { attempt ->
            _connectionState.value = BleConnectionState.CONNECTING
            val deferred = CompletableDeferred<Boolean>()
            pendingConnect = deferred

            val gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            activeGatt = gatt

            val connected = withTimeoutOrNull(GATT_CONNECT_TIMEOUT_MS) { deferred.await() } ?: false
            pendingConnect = null

            if (connected) {
                subscribeToTelemetry()
                return true
            }

            cleanupGatt()
            if (attempt < MAX_CONNECT_RETRIES) {
                delay(RETRY_DELAY_MS)
            }
        }

        _connectionState.value = BleConnectionState.ERROR
        if (_lastError.value == null) {
            _lastError.value = "Failed to connect to $address"
        }
        return false
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        pendingConnect?.cancel()
        pendingConnect = null
        pendingGattOp?.cancel()
        pendingGattOp = null
        cleanupGatt()
        _connectionState.value = BleConnectionState.DISCONNECTED
    }

    override suspend fun sendLightingCommand(frame: LightingControlFrame): Boolean {
        val char = lightingChar ?: return false
        return writeCharacteristicSerialized(char, frame.toByteArray())
    }

    override suspend fun sendAudioCommand(frame: AudioControlFrame): Boolean {
        val char = audioChar ?: return false
        return writeCharacteristicSerialized(char, frame.toByteArray())
    }

    @SuppressLint("MissingPermission")
    private suspend fun subscribeToTelemetry(): Boolean {
        val gatt = activeGatt ?: return false
        val char = telemetryChar ?: return false
        if (!gatt.setCharacteristicNotification(char, true)) {
            return false
        }
        val cccd = char.getDescriptor(GattProfile.CCCD_UUID) ?: return false
        return writeDescriptorSerialized(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
    }

    @SuppressLint("MissingPermission")
    private suspend fun writeCharacteristicSerialized(
        characteristic: BluetoothGattCharacteristic,
        payload: ByteArray
    ): Boolean = gattMutex.withLock {
        val gatt = activeGatt ?: return false
        if (_connectionState.value != BleConnectionState.CONNECTED) return false

        val deferred = CompletableDeferred<Boolean>()
        pendingGattOp = deferred

        val initiated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(
                characteristic,
                payload,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            ) == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = payload
            @Suppress("DEPRECATION")
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }

        if (!initiated) {
            pendingGattOp = null
            return false
        }

        val ok = withTimeoutOrNull(GATT_OP_TIMEOUT_MS) { deferred.await() } ?: false
        pendingGattOp = null
        return ok
    }

    @SuppressLint("MissingPermission")
    private suspend fun writeDescriptorSerialized(
        descriptor: BluetoothGattDescriptor,
        payload: ByteArray
    ): Boolean = gattMutex.withLock {
        val gatt = activeGatt ?: return false
        val deferred = CompletableDeferred<Boolean>()
        pendingGattOp = deferred

        val initiated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, payload) == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = payload
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }

        if (!initiated) {
            pendingGattOp = null
            return false
        }

        val ok = withTimeoutOrNull(GATT_OP_TIMEOUT_MS) { deferred.await() } ?: false
        pendingGattOp = null
        return ok
    }

    @SuppressLint("MissingPermission")
    private fun cleanupGatt() {
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (_: SecurityException) {
        }
        activeGatt = null
        lightingChar = null
        audioChar = null
        telemetryChar = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _lastError.value = "GATT connection error (status=$status)"
                pendingConnect?.complete(false)
                if (_connectionState.value == BleConnectionState.CONNECTED) {
                    _connectionState.value = BleConnectionState.DISCONNECTED
                }
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = BleConnectionState.DISCOVERING_SERVICES
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    pendingConnect?.complete(false)
                    _connectionState.value = BleConnectionState.DISCONNECTED
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _lastError.value = "Service discovery failed (status=$status)"
                pendingConnect?.complete(false)
                return
            }

            val service = gatt.getService(GattProfile.SERVICE_UUID)
            if (service == null) {
                _lastError.value = "XmasTree GATT service not found"
                pendingConnect?.complete(false)
                return
            }

            lightingChar = service.getCharacteristic(GattProfile.LIGHTING_CONTROL_CHARACTERISTIC_UUID)
            audioChar = service.getCharacteristic(GattProfile.AUDIO_CONTROL_CHARACTERISTIC_UUID)
            telemetryChar = service.getCharacteristic(GattProfile.TELEMETRY_CHARACTERISTIC_UUID)

            if (lightingChar == null || audioChar == null || telemetryChar == null) {
                _lastError.value = "Required XmasTree characteristics missing"
                pendingConnect?.complete(false)
                return
            }

            _connectionState.value = BleConnectionState.CONNECTED
            pendingConnect?.complete(true)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            pendingGattOp?.complete(status == BluetoothGatt.GATT_SUCCESS)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            pendingGattOp?.complete(status == BluetoothGatt.GATT_SUCCESS)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleTelemetryBytes(characteristic.uuid, value)
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            val value = characteristic.value ?: return
            handleTelemetryBytes(characteristic.uuid, value)
        }
    }

    private fun handleTelemetryBytes(uuid: UUID, bytes: ByteArray) {
        if (uuid != GattProfile.TELEMETRY_CHARACTERISTIC_UUID) return
        try {
            _telemetry.value = TelemetryFrame.fromByteArray(bytes)
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun hasScanPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val GATT_CONNECT_TIMEOUT_MS = 10_000L
        private const val GATT_OP_TIMEOUT_MS = 3_000L
        private const val RETRY_DELAY_MS = 500L
        private const val MAX_CONNECT_RETRIES = 2
    }
}
