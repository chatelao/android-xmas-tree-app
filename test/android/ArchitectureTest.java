package test.android;

import com.xmastree.remote.ble.BleManager;
import com.xmastree.remote.data.Preset;
import com.xmastree.remote.protocol.ProtocolSpec.*;
import com.xmastree.remote.ui.MainViewModel;

public class ArchitectureTest {
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("Running Android Architecture Unit Tests");
        System.out.println("==========================================");

        testBleManager();
        testPresetRepository();
        testMainViewModel();

        System.out.println("==========================================");
        System.out.println("All Android Architecture Tests Passed Successfully!");
        System.out.println("==========================================");
    }

    private static void testBleManager() {
        System.out.println("Testing BleManager stub...");
        BleManager manager = new BleManager();
        assert manager.getConnectionState() == BleManager.ConnectionState.DISCONNECTED : "Initial state must be DISCONNECTED";
        assert !manager.isScanning() : "Initial scanning state must be false";

        manager.startScan();
        assert manager.isScanning() : "Scanning state must be true";
        manager.stopScan();
        assert !manager.isScanning() : "Scanning state must be false";

        boolean connected = manager.connect("AA:BB:CC:DD:EE:FF");
        assert connected : "Connect should return true";
        assert manager.getConnectionState() == BleManager.ConnectionState.CONNECTED : "State must be CONNECTED";

        LightingControlFrame frame = new LightingControlFrame(
            LightingCommand.SET_PATTERN,
            PatternId.STATIC,
            255, 255, 255, 100, 50, 0
        );
        assert manager.sendLightingCommand(frame) : "sendLightingCommand should return true when connected";

        manager.disconnect();
        assert manager.getConnectionState() == BleManager.ConnectionState.DISCONNECTED : "State must be DISCONNECTED";
        assert !manager.sendLightingCommand(frame) : "sendLightingCommand should return false when disconnected";

        System.out.println("  PASSED: BleManager tests");
    }

    private static void testPresetRepository() {
        System.out.println("Testing Preset Repository stub...");
        Preset.Repository repository = new Preset.Repository();
        assert repository.getAllPresets().isEmpty() : "Presets list should initially be empty";

        Preset preset = new Preset(
            "p1", "Festive Red",
            PatternId.STATIC,
            255, 0, 0,
            100, 50,
            MelodyId.JINGLE_BELLS,
            80, 100
        );

        assert repository.savePreset(preset) : "savePreset should return true";
        assert repository.getAllPresets().size() == 1 : "Preset list count should be 1";
        assert repository.getPresetById("p1") != null : "Preset p1 should be retrievable";
        assert repository.getPresetById("p1").getName().equals("Festive Red") : "Preset name should match";

        assert repository.deletePreset("p1") : "deletePreset should return true";
        assert repository.getAllPresets().isEmpty() : "Presets list should be empty after delete";

        System.out.println("  PASSED: Preset Repository tests");
    }

    private static void testMainViewModel() {
        System.out.println("Testing MainViewModel stub...");
        MainViewModel viewModel = new MainViewModel();
        MainViewModel.UiState state = viewModel.getUiState();

        assert state.getConnectionState() == BleManager.ConnectionState.DISCONNECTED : "Initial VM state DISCONNECTED";

        viewModel.connectDevice("AA:BB:CC:DD:EE:FF");
        assert state.getConnectionState() == BleManager.ConnectionState.CONNECTED : "VM state CONNECTED after connect";

        Preset preset = new Preset(
            "p2", "Rainbow",
            PatternId.RAINBOW_WAVE,
            0, 255, 0,
            90, 80,
            MelodyId.WE_WISH_YOU,
            70, 110
        );

        viewModel.applyPreset(preset);
        assert state.getSelectedPattern() == PatternId.RAINBOW_WAVE : "Selected pattern match preset";
        assert state.getGreen() == 255 : "Green match preset";
        assert state.getBrightness() == 90 : "Brightness match preset";
        assert state.getSelectedMelody() == MelodyId.WE_WISH_YOU : "Melody match preset";

        viewModel.disconnectDevice();
        assert state.getConnectionState() == BleManager.ConnectionState.DISCONNECTED : "VM state DISCONNECTED";

        System.out.println("  PASSED: MainViewModel tests");
    }
}
