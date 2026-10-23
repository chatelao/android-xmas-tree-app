package test.android;

import com.xmastree.remote.protocol.GattProfile;
import com.xmastree.remote.protocol.ProtocolSpec.*;

public class ProtocolTest {
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("Running Android Protocol Unit Tests");
        System.out.println("==========================================");

        testGattProfile();
        testLightingControlFrame();
        testAudioControlFrame();
        testTelemetryFrame();

        System.out.println("==========================================");
        System.out.println("All Android Protocol Tests Passed Successfully!");
        System.out.println("==========================================");
    }

    private static void testGattProfile() {
        System.out.println("Testing GATT Profile UUIDs...");
        assert GattProfile.DEVICE_NAME.equals("XmasTree-ESP32C6") : "Device Name mismatch";
        assert GattProfile.SERVICE_UUID.toString().equals("12345678-1234-5678-1234-56789abcdef0") : "Service UUID mismatch";
        assert GattProfile.LIGHTING_CONTROL_CHARACTERISTIC_UUID.toString().equals("12345678-1234-5678-1234-56789abcdef1") : "Lighting UUID mismatch";
        assert GattProfile.AUDIO_CONTROL_CHARACTERISTIC_UUID.toString().equals("12345678-1234-5678-1234-56789abcdef2") : "Audio UUID mismatch";
        assert GattProfile.TELEMETRY_CHARACTERISTIC_UUID.toString().equals("12345678-1234-5678-1234-56789abcdef3") : "Telemetry UUID mismatch";
        System.out.println("  PASSED: GATT Profile UUID tests");
    }

    private static void testLightingControlFrame() {
        System.out.println("Testing Lighting Control Frame...");
        LightingControlFrame frame = new LightingControlFrame(
            LightingCommand.SET_PATTERN,
            PatternId.RAINBOW_WAVE,
            255, 0, 128, 50, 75, 0
        );
        byte[] bytes = frame.toByteArray();
        assert bytes.length == 8 : "Frame length must be 8";
        assert bytes[0] == 0x01 : "Command byte mismatch";
        assert bytes[1] == 0x02 : "Pattern byte mismatch";
        assert (bytes[2] & 0xFF) == 255 : "Red byte mismatch";
        assert (bytes[3] & 0xFF) == 0 : "Green byte mismatch";
        assert (bytes[4] & 0xFF) == 128 : "Blue byte mismatch";
        assert (bytes[5] & 0xFF) == 50 : "Brightness byte mismatch";
        assert (bytes[6] & 0xFF) == 75 : "Speed byte mismatch";

        LightingControlFrame decoded = LightingControlFrame.fromByteArray(bytes);
        assert decoded.equals(frame) : "Decoded frame must equal original";
        System.out.println("  PASSED: Lighting Control Frame tests");
    }

    private static void testAudioControlFrame() {
        System.out.println("Testing Audio Control Frame...");
        AudioControlFrame frame = new AudioControlFrame(
            AudioCommand.PLAY_SONG,
            MelodyId.JINGLE_BELLS,
            PlaybackState.PLAY,
            80, 100, 0
        );
        byte[] bytes = frame.toByteArray();
        assert bytes.length == 6 : "Frame length must be 6";
        assert bytes[0] == 0x10 : "Command byte mismatch";
        assert bytes[1] == 0x01 : "Melody byte mismatch";
        assert bytes[2] == 0x01 : "Playback state mismatch";
        assert (bytes[3] & 0xFF) == 80 : "Volume byte mismatch";
        assert (bytes[4] & 0xFF) == 100 : "Tempo byte mismatch";

        AudioControlFrame decoded = AudioControlFrame.fromByteArray(bytes);
        assert decoded.equals(frame) : "Decoded frame must equal original";
        System.out.println("  PASSED: Audio Control Frame tests");
    }

    private static void testTelemetryFrame() {
        System.out.println("Testing Telemetry Frame...");
        TelemetryFrame frame = new TelemetryFrame(
            0x12345678L,
            PatternId.SPARKLE,
            MelodyId.SILENT_NIGHT,
            90, 40, true, ErrorCode.OK
        );
        byte[] bytes = frame.toByteArray();
        assert bytes.length == 10 : "Frame length must be 10";
        assert (bytes[0] & 0xFF) == 0x78 : "Uptime byte 0 mismatch";
        assert (bytes[1] & 0xFF) == 0x56 : "Uptime byte 1 mismatch";
        assert (bytes[2] & 0xFF) == 0x34 : "Uptime byte 2 mismatch";
        assert (bytes[3] & 0xFF) == 0x12 : "Uptime byte 3 mismatch";

        TelemetryFrame decoded = TelemetryFrame.fromByteArray(bytes);
        assert decoded.equals(frame) : "Decoded frame must equal original";
        System.out.println("  PASSED: Telemetry Frame tests");
    }
}
