package com.xmastree.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xmastree.remote.ble.BleClient
import com.xmastree.remote.ble.BleConnectionState
import com.xmastree.remote.ble.ScannedDevice
import com.xmastree.remote.ble.SwitchableBleClient
import com.xmastree.remote.data.PresetStore
import com.xmastree.remote.data.TreePreset
import com.xmastree.remote.protocol.AudioCommand
import com.xmastree.remote.protocol.AudioControlFrame
import com.xmastree.remote.protocol.LightingCommand
import com.xmastree.remote.protocol.LightingControlFrame
import com.xmastree.remote.protocol.MelodyId
import com.xmastree.remote.protocol.PatternId
import com.xmastree.remote.protocol.PlaybackState
import com.xmastree.remote.protocol.TelemetryFrame
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TreeRemoteUiState(
    val isSimulationMode: Boolean = false,
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val connectedDeviceAddress: String? = null,
    val scannedDevices: List<ScannedDevice> = emptyList(),
    val patternId: PatternId = PatternId.RAINBOW_WAVE,
    val red: Int = 255,
    val green: Int = 50,
    val blue: Int = 50,
    val brightness: Int = 80,
    val speed: Int = 50,
    val melodyId: MelodyId = MelodyId.JINGLE_BELLS,
    val playbackState: PlaybackState = PlaybackState.STOP,
    val volume: Int = 70,
    val tempo: Int = 100,
    val telemetry: TelemetryFrame? = null,
    val presets: List<TreePreset> = emptyList(),
    val statusMessage: String? = null
)

class TreeRemoteViewModel(
    private val bleClient: BleClient,
    private val presetStore: PresetStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TreeRemoteUiState())
    val uiState: StateFlow<TreeRemoteUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        if (bleClient is SwitchableBleClient) {
            bleClient.isSimulationMode
                .onEach { sim ->
                    _uiState.update { it.copy(isSimulationMode = sim) }
                }
                .launchIn(viewModelScope)
        }

        bleClient.connectionState
            .onEach { state ->
                _uiState.update {
                    it.copy(
                        connectionState = state,
                        connectedDeviceAddress = if (state == BleConnectionState.DISCONNECTED) null else it.connectedDeviceAddress
                    )
                }
            }
            .launchIn(viewModelScope)

        bleClient.scannedDevices
            .onEach { devices ->
                _uiState.update { it.copy(scannedDevices = devices) }
            }
            .launchIn(viewModelScope)

        bleClient.telemetry
            .onEach { telemetry ->
                if (telemetry != null) {
                    _uiState.update {
                        it.copy(
                            telemetry = telemetry,
                            patternId = telemetry.currentPatternId,
                            melodyId = telemetry.currentMelodyId,
                            brightness = telemetry.currentBrightness,
                            volume = telemetry.currentVolume
                        )
                    }
                }
            }
            .launchIn(viewModelScope)

        bleClient.lastError
            .onEach { err ->
                if (!err.isNullOrBlank()) {
                    _uiState.update { it.copy(statusMessage = err) }
                }
            }
            .launchIn(viewModelScope)

        presetStore.presets
            .onEach { list ->
                _uiState.update { it.copy(presets = list) }
            }
            .launchIn(viewModelScope)
    }

    fun setSimulationMode(enabled: Boolean) {
        stopScan()
        viewModelScope.launch {
            (bleClient as? SwitchableBleClient)?.setSimulationMode(enabled)
            _uiState.update {
                it.copy(
                    connectedDeviceAddress = null,
                    telemetry = null,
                    statusMessage = if (enabled) "Simulation mode enabled (no BLE required)" else "Bluetooth hardware mode enabled"
                )
            }
        }
    }

    fun startScan() {
        scanJob?.cancel()
        scanJob = bleClient.scanForDevices()
            .catch { e -> _uiState.update { it.copy(statusMessage = "Scan error: ${e.message}") } }
            .launchIn(viewModelScope)
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
    }

    fun connect(address: String) {
        stopScan()
        viewModelScope.launch {
            val ok = bleClient.connect(address)
            _uiState.update {
                it.copy(
                    connectedDeviceAddress = if (ok) address else null,
                    statusMessage = if (ok) "Connected to $address" else "Connection failed"
                )
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            bleClient.disconnect()
            _uiState.update {
                it.copy(
                    connectedDeviceAddress = null,
                    telemetry = null,
                    statusMessage = "Disconnected"
                )
            }
        }
    }

    fun selectPattern(patternId: PatternId) {
        _uiState.update { it.copy(patternId = patternId) }
        dispatchLighting(LightingCommand.SET_PATTERN)
    }

    fun setColor(red: Int, green: Int, blue: Int) {
        _uiState.update {
            it.copy(
                patternId = if (it.patternId == PatternId.OFF) PatternId.STATIC else it.patternId,
                red = red.coerceIn(0, 255),
                green = green.coerceIn(0, 255),
                blue = blue.coerceIn(0, 255)
            )
        }
        dispatchLighting(LightingCommand.SET_STATIC_COLOR)
    }

    fun setBrightness(brightness: Int) {
        _uiState.update { it.copy(brightness = brightness.coerceIn(0, 100)) }
        dispatchLighting(LightingCommand.SET_BRIGHTNESS)
    }

    fun setSpeed(speed: Int) {
        _uiState.update { it.copy(speed = speed.coerceIn(1, 100)) }
        dispatchLighting(LightingCommand.SET_PATTERN)
    }

    fun selectMelody(melodyId: MelodyId) {
        _uiState.update { it.copy(melodyId = melodyId) }
        if (_uiState.value.playbackState == PlaybackState.PLAY) {
            dispatchAudio(AudioCommand.PLAY_SONG)
        }
    }

    fun playAudio() {
        val melody = if (_uiState.value.melodyId == MelodyId.SILENT) MelodyId.JINGLE_BELLS else _uiState.value.melodyId
        _uiState.update { it.copy(melodyId = melody, playbackState = PlaybackState.PLAY) }
        dispatchAudio(AudioCommand.PLAY_SONG)
    }

    fun pauseAudio() {
        _uiState.update { it.copy(playbackState = PlaybackState.PAUSE) }
        dispatchAudio(AudioCommand.PLAY_SONG)
    }

    fun stopAudio() {
        _uiState.update { it.copy(playbackState = PlaybackState.STOP) }
        dispatchAudio(AudioCommand.STOP_AUDIO)
    }

    fun setVolume(volume: Int) {
        _uiState.update { it.copy(volume = volume.coerceIn(0, 100)) }
        dispatchAudio(AudioCommand.SET_VOLUME)
    }

    fun setTempo(tempo: Int) {
        _uiState.update { it.copy(tempo = tempo.coerceIn(50, 200)) }
        dispatchAudio(AudioCommand.PLAY_SONG)
    }

    fun saveCurrentAsPreset(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val s = _uiState.value
        val preset = TreePreset(
            name = trimmed,
            patternId = s.patternId,
            red = s.red,
            green = s.green,
            blue = s.blue,
            brightness = s.brightness,
            speed = s.speed,
            melodyId = s.melodyId,
            volume = s.volume,
            tempo = s.tempo
        )
        viewModelScope.launch {
            presetStore.savePreset(preset)
            _uiState.update { it.copy(statusMessage = "Saved preset \"$trimmed\"") }
        }
    }

    fun applyPreset(preset: TreePreset) {
        _uiState.update {
            it.copy(
                patternId = preset.patternId,
                red = preset.red,
                green = preset.green,
                blue = preset.blue,
                brightness = preset.brightness,
                speed = preset.speed,
                melodyId = preset.melodyId,
                playbackState = if (preset.melodyId == MelodyId.SILENT) PlaybackState.STOP else PlaybackState.PLAY,
                volume = preset.volume,
                tempo = preset.tempo,
                statusMessage = "Applied preset \"${preset.name}\""
            )
        }
        dispatchLighting(LightingCommand.SET_PATTERN)
        dispatchAudio(
            if (preset.melodyId == MelodyId.SILENT) AudioCommand.STOP_AUDIO else AudioCommand.PLAY_SONG
        )
    }

    fun deletePreset(presetId: String) {
        viewModelScope.launch {
            presetStore.deletePreset(presetId)
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun dispatchLighting(command: LightingCommand) {
        val s = _uiState.value
        val frame = LightingControlFrame(
            command = command,
            patternId = s.patternId,
            red = s.red,
            green = s.green,
            blue = s.blue,
            brightness = s.brightness,
            speed = s.speed
        )
        viewModelScope.launch {
            bleClient.sendLightingCommand(frame)
        }
    }

    private fun dispatchAudio(command: AudioCommand) {
        val s = _uiState.value
        val frame = AudioControlFrame(
            command = command,
            melodyId = s.melodyId,
            playbackState = s.playbackState,
            volume = s.volume,
            tempo = s.tempo
        )
        viewModelScope.launch {
            bleClient.sendAudioCommand(frame)
        }
    }

    class Factory(
        private val bleClient: BleClient,
        private val presetStore: PresetStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TreeRemoteViewModel(bleClient, presetStore) as T
        }
    }
}
