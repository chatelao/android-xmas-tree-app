package com.xmastree.remote.ble

import com.xmastree.remote.protocol.AudioControlFrame
import com.xmastree.remote.protocol.ErrorCode
import com.xmastree.remote.protocol.LightingControlFrame
import com.xmastree.remote.protocol.MelodyId
import com.xmastree.remote.protocol.PatternId
import com.xmastree.remote.protocol.TelemetryFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class SimulatedBleClient(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : BleClient {

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices.asStateFlow()

    private val _telemetry = MutableStateFlow<TelemetryFrame?>(null)
    override val telemetry: StateFlow<TelemetryFrame?> = _telemetry.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    override val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var currentPattern = PatternId.RAINBOW_WAVE
    private var currentMelody = MelodyId.SILENT
    private var currentBrightness = 80
    private var currentVolume = 60
    private var uptimeSeconds = 0L
    private var telemetryJob: Job? = null

    override fun scanForDevices(): Flow<ScannedDevice> = flow {
        _lastError.value = null
        _scannedDevices.value = emptyList()
        _connectionState.value = BleConnectionState.SCANNING
        delay(250)
        val virtualDevice = ScannedDevice(
            name = SIMULATED_DEVICE_NAME,
            address = SIMULATED_DEVICE_ADDRESS,
            rssi = -42
        )
        _scannedDevices.value = listOf(virtualDevice)
        emit(virtualDevice)
        if (_connectionState.value == BleConnectionState.SCANNING) {
            _connectionState.value = BleConnectionState.DISCONNECTED
        }
    }

    override suspend fun connect(address: String): Boolean {
        if (address.isBlank()) {
            _lastError.value = "Invalid simulated address"
            _connectionState.value = BleConnectionState.ERROR
            return false
        }
        _lastError.value = null
        _connectionState.value = BleConnectionState.CONNECTING
        delay(150)
        _connectionState.value = BleConnectionState.DISCOVERING_SERVICES
        delay(150)
        _connectionState.value = BleConnectionState.CONNECTED
        startTelemetryLoop()
        return true
    }

    override suspend fun disconnect() {
        telemetryJob?.cancel()
        telemetryJob = null
        _telemetry.value = null
        _connectionState.value = BleConnectionState.DISCONNECTED
    }

    override suspend fun sendLightingCommand(frame: LightingControlFrame): Boolean {
        if (_connectionState.value != BleConnectionState.CONNECTED) return false
        currentPattern = frame.patternId
        currentBrightness = frame.brightness
        emitTelemetrySnapshot()
        return true
    }

    override suspend fun sendAudioCommand(frame: AudioControlFrame): Boolean {
        if (_connectionState.value != BleConnectionState.CONNECTED) return false
        currentMelody = frame.melodyId
        currentVolume = frame.volume
        emitTelemetrySnapshot()
        return true
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        uptimeSeconds = 0L
        emitTelemetrySnapshot()
        telemetryJob = scope.launch {
            while (_connectionState.value == BleConnectionState.CONNECTED) {
                delay(1000)
                if (_connectionState.value == BleConnectionState.CONNECTED) {
                    uptimeSeconds += 1
                    emitTelemetrySnapshot()
                }
            }
        }
    }

    private fun emitTelemetrySnapshot() {
        _telemetry.value = TelemetryFrame(
            systemUptimeSeconds = uptimeSeconds,
            currentPatternId = currentPattern,
            currentMelodyId = currentMelody,
            currentBrightness = currentBrightness,
            currentVolume = currentVolume,
            isConnected = _connectionState.value == BleConnectionState.CONNECTED,
            errorCode = ErrorCode.OK
        )
    }

    companion object {
        const val SIMULATED_DEVICE_NAME = "XmasTree-ESP32C6 (Sim)"
        const val SIMULATED_DEVICE_ADDRESS = "SIM:00:11:22:33:44:55"
    }
}
