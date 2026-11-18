package com.xmastree.remote.ble

import com.xmastree.remote.protocol.AudioControlFrame
import com.xmastree.remote.protocol.LightingControlFrame
import com.xmastree.remote.protocol.TelemetryFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class SwitchableBleClient(
    private val realClient: BleClient,
    private val simulatedClient: BleClient = SimulatedBleClient(),
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : BleClient {

    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private fun activeClient(): BleClient =
        if (_isSimulationMode.value) simulatedClient else realClient

    override val connectionState: StateFlow<BleConnectionState> =
        _isSimulationMode.flatMapLatest { sim ->
            if (sim) simulatedClient.connectionState else realClient.connectionState
        }.stateIn(scope, SharingStarted.Eagerly, BleConnectionState.DISCONNECTED)

    override val scannedDevices: StateFlow<List<ScannedDevice>> =
        _isSimulationMode.flatMapLatest { sim ->
            if (sim) simulatedClient.scannedDevices else realClient.scannedDevices
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val telemetry: StateFlow<TelemetryFrame?> =
        _isSimulationMode.flatMapLatest { sim ->
            if (sim) simulatedClient.telemetry else realClient.telemetry
        }.stateIn(scope, SharingStarted.Eagerly, null)

    override val lastError: StateFlow<String?> =
        _isSimulationMode.flatMapLatest { sim ->
            if (sim) simulatedClient.lastError else realClient.lastError
        }.stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun setSimulationMode(enabled: Boolean) {
        if (_isSimulationMode.value == enabled) return
        activeClient().disconnect()
        _isSimulationMode.value = enabled
    }

    override fun scanForDevices(): Flow<ScannedDevice> = activeClient().scanForDevices()

    override suspend fun connect(address: String): Boolean = activeClient().connect(address)

    override suspend fun disconnect() = activeClient().disconnect()

    override suspend fun sendLightingCommand(frame: LightingControlFrame): Boolean =
        activeClient().sendLightingCommand(frame)

    override suspend fun sendAudioCommand(frame: AudioControlFrame): Boolean =
        activeClient().sendAudioCommand(frame)
}
