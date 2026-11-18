package com.xmastree.remote

import com.xmastree.remote.ble.BleClient
import com.xmastree.remote.ble.BleConnectionState
import com.xmastree.remote.ble.ScannedDevice
import com.xmastree.remote.data.PresetStore
import com.xmastree.remote.data.TreePreset
import com.xmastree.remote.protocol.AudioCommand
import com.xmastree.remote.protocol.AudioControlFrame
import com.xmastree.remote.protocol.ErrorCode
import com.xmastree.remote.protocol.LightingCommand
import com.xmastree.remote.protocol.LightingControlFrame
import com.xmastree.remote.protocol.MelodyId
import com.xmastree.remote.protocol.PatternId
import com.xmastree.remote.protocol.PlaybackState
import com.xmastree.remote.protocol.TelemetryFrame
import com.xmastree.remote.ui.TreeRemoteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class FakeBleClient : BleClient {
    override val connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    override val scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    override val telemetry = MutableStateFlow<TelemetryFrame?>(null)
    override val lastError = MutableStateFlow<String?>(null)

    val sentLightingFrames = mutableListOf<LightingControlFrame>()
    val sentAudioFrames = mutableListOf<AudioControlFrame>()

    override fun scanForDevices(): Flow<ScannedDevice> = emptyFlow()

    override suspend fun connect(address: String): Boolean {
        connectionState.value = BleConnectionState.CONNECTED
        return true
    }

    override suspend fun disconnect() {
        connectionState.value = BleConnectionState.DISCONNECTED
    }

    override suspend fun sendLightingCommand(frame: LightingControlFrame): Boolean {
        sentLightingFrames.add(frame)
        return true
    }

    override suspend fun sendAudioCommand(frame: AudioControlFrame): Boolean {
        sentAudioFrames.add(frame)
        return true
    }
}

private class FakePresetStore : PresetStore {
    override val presets = MutableStateFlow<List<TreePreset>>(emptyList())

    override suspend fun savePreset(preset: TreePreset) {
        presets.value = presets.value.filterNot { it.id == preset.id } + preset
    }

    override suspend fun deletePreset(presetId: String) {
        presets.value = presets.value.filterNot { it.id == presetId }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProtocolAndViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `lighting, audio, and telemetry frames round-trip cleanly`() {
        val lighting = LightingControlFrame(
            command = LightingCommand.SET_PATTERN,
            patternId = PatternId.SPARKLE,
            red = 255, green = 128, blue = 64,
            brightness = 80, speed = 50
        )
        val decodedLighting = LightingControlFrame.fromByteArray(lighting.toByteArray())
        assertEquals(lighting, decodedLighting)

        val audio = AudioControlFrame(
            command = AudioCommand.PLAY_SONG,
            melodyId = MelodyId.JINGLE_BELLS,
            playbackState = PlaybackState.PLAY,
            volume = 75, tempo = 110
        )
        val decodedAudio = AudioControlFrame.fromByteArray(audio.toByteArray())
        assertEquals(audio, decodedAudio)

        val telemetry = TelemetryFrame(
            systemUptimeSeconds = 258L,
            currentPatternId = PatternId.TWINKLE,
            currentMelodyId = MelodyId.SILENT_NIGHT,
            currentBrightness = 85,
            currentVolume = 70,
            isConnected = true,
            errorCode = ErrorCode.OK
        )
        val bytes = telemetry.toByteArray()
        assertArrayEquals(
            byteArrayOf(0x02, 0x01, 0x00, 0x00, 0x05, 0x03, 0x55, 0x46, 0x01, 0x00),
            bytes
        )
        assertEquals(telemetry, TelemetryFrame.fromByteArray(bytes))
    }

    @Test
    fun `viewModel dispatches commands, syncs telemetry, and manages presets`() = runTest {
        val ble = FakeBleClient()
        val store = FakePresetStore()
        val vm = TreeRemoteViewModel(ble, store)

        vm.connect("AA:BB:CC:DD:EE:FF")
        advanceUntilIdle()
        assertEquals(BleConnectionState.CONNECTED, vm.uiState.value.connectionState)
        assertEquals("AA:BB:CC:DD:EE:FF", vm.uiState.value.connectedDeviceAddress)

        vm.selectPattern(PatternId.FESTIVE_CANDLE)
        vm.setColor(255, 100, 20)
        vm.setBrightness(90)
        vm.playAudio()
        advanceUntilIdle()

        assertEquals(3, ble.sentLightingFrames.size)
        assertEquals(90, ble.sentLightingFrames.last().brightness)
        assertEquals(1, ble.sentAudioFrames.size)
        assertEquals(PlaybackState.PLAY, ble.sentAudioFrames.last().playbackState)

        // Incoming BLE telemetry updates UI state
        ble.telemetry.value = TelemetryFrame(
            systemUptimeSeconds = 42L,
            currentPatternId = PatternId.TWINKLE,
            currentMelodyId = MelodyId.DECK_THE_HALLS,
            currentBrightness = 60,
            currentVolume = 55,
            isConnected = true,
            errorCode = ErrorCode.OK
        )
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.telemetry)
        assertEquals(PatternId.TWINKLE, vm.uiState.value.patternId)
        assertEquals(MelodyId.DECK_THE_HALLS, vm.uiState.value.melodyId)
        assertEquals(60, vm.uiState.value.brightness)
        assertEquals(55, vm.uiState.value.volume)

        // Save and apply preset
        vm.saveCurrentAsPreset("Tree Party")
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.presets.size)

        vm.applyPreset(vm.uiState.value.presets.first())
        advanceUntilIdle()
        assertEquals(4, ble.sentLightingFrames.size)
        assertEquals(2, ble.sentAudioFrames.size)
    }

    @Test
    fun `simulation mode discovers virtual tree, connects, and updates telemetry`() = runTest {
        val realClient = FakeBleClient()
        val simClient = com.xmastree.remote.ble.SimulatedBleClient(scope = backgroundScope)
        val switchable = com.xmastree.remote.ble.SwitchableBleClient(
            realClient = realClient,
            simulatedClient = simClient,
            scope = backgroundScope
        )
        val store = FakePresetStore()
        val vm = TreeRemoteViewModel(switchable, store)

        vm.setSimulationMode(true)
        advanceUntilIdle()
        assertEquals(true, vm.uiState.value.isSimulationMode)

        val discovered = mutableListOf<ScannedDevice>()
        switchable.scanForDevices().collect { discovered.add(it) }
        assertEquals(1, discovered.size)
        assertEquals(com.xmastree.remote.ble.SimulatedBleClient.SIMULATED_DEVICE_ADDRESS, discovered.first().address)

        vm.connect(discovered.first().address)
        advanceUntilIdle()
        assertEquals(BleConnectionState.CONNECTED, vm.uiState.value.connectionState)

        vm.selectPattern(PatternId.SPARKLE)
        vm.setBrightness(77)
        vm.playAudio()
        vm.setVolume(65)
        advanceUntilIdle()

        val telemetry = vm.uiState.value.telemetry
        assertNotNull(telemetry)
        assertEquals(PatternId.SPARKLE, telemetry!!.currentPatternId)
        assertEquals(77, telemetry.currentBrightness)
        assertEquals(65, telemetry.currentVolume)
        assertEquals(0, realClient.sentLightingFrames.size)

        vm.disconnect()
        advanceUntilIdle()
        assertEquals(BleConnectionState.DISCONNECTED, vm.uiState.value.connectionState)
    }
}
