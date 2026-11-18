package com.xmastree.remote.ble

import com.xmastree.remote.protocol.AudioControlFrame
import com.xmastree.remote.protocol.LightingControlFrame
import com.xmastree.remote.protocol.TelemetryFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class BleConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    DISCOVERING_SERVICES,
    CONNECTED,
    ERROR
}

data class ScannedDevice(
    val name: String,
    val address: String,
    val rssi: Int
)

interface BleClient {
    val connectionState: StateFlow<BleConnectionState>
    val scannedDevices: StateFlow<List<ScannedDevice>>
    val telemetry: StateFlow<TelemetryFrame?>
    val lastError: StateFlow<String?>

    fun scanForDevices(): Flow<ScannedDevice>
    suspend fun connect(address: String): Boolean
    suspend fun disconnect()
    suspend fun sendLightingCommand(frame: LightingControlFrame): Boolean
    suspend fun sendAudioCommand(frame: AudioControlFrame): Boolean
}
