package com.xmastree.remote.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public final class ProtocolSpec {

    public enum LightingCommand {
        SET_PATTERN((byte) 0x01),
        SET_STATIC_COLOR((byte) 0x02),
        SET_BRIGHTNESS((byte) 0x03);

        private final byte id;
        LightingCommand(byte id) { this.id = id; }
        public byte getId() { return id; }

        public static LightingCommand fromByte(byte b) {
            for (LightingCommand cmd : values()) {
                if (cmd.id == b) return cmd;
            }
            return null;
        }
    }

    public enum PatternId {
        OFF((byte) 0),
        STATIC((byte) 1),
        RAINBOW_WAVE((byte) 2),
        SPARKLE((byte) 3),
        FESTIVE_CANDLE((byte) 4),
        TWINKLE((byte) 5);

        private final byte id;
        PatternId(byte id) { this.id = id; }
        public byte getId() { return id; }

        public static PatternId fromByte(byte b) {
            for (PatternId p : values()) {
                if (p.id == b) return p;
            }
            return null;
        }
    }

    public enum AudioCommand {
        PLAY_SONG((byte) 0x10),
        STOP_AUDIO((byte) 0x11),
        PLAY_FREQ_TONE((byte) 0x12),
        SET_VOLUME((byte) 0x13);

        private final byte id;
        AudioCommand(byte id) { this.id = id; }
        public byte getId() { return id; }

        public static AudioCommand fromByte(byte b) {
            for (AudioCommand cmd : values()) {
                if (cmd.id == b) return cmd;
            }
            return null;
        }
    }

    public enum MelodyId {
        SILENT((byte) 0),
        JINGLE_BELLS((byte) 1),
        WE_WISH_YOU((byte) 2),
        SILENT_NIGHT((byte) 3),
        DECK_THE_HALLS((byte) 4);

        private final byte id;
        MelodyId(byte id) { this.id = id; }
        public byte getId() { return id; }

        public static MelodyId fromByte(byte b) {
            for (MelodyId m : values()) {
                if (m.id == b) return m;
            }
            return null;
        }
    }

    public enum PlaybackState {
        STOP((byte) 0x00),
        PLAY((byte) 0x01),
        PAUSE((byte) 0x02);

        private final byte id;
        PlaybackState(byte id) { this.id = id; }
        public byte getId() { return id; }

        public static PlaybackState fromByte(byte b) {
            for (PlaybackState ps : values()) {
                if (ps.id == b) return ps;
            }
            return null;
        }
    }

    public enum ErrorCode {
        OK((byte) 0x00),
        NVS_ERROR((byte) 0x01),
        LED_TIMING_ERROR((byte) 0x02);

        private final byte id;
        ErrorCode(byte id) { this.id = id; }
        public byte getId() { return id; }

        public static ErrorCode fromByte(byte b) {
            for (ErrorCode e : values()) {
                if (e.id == b) return e;
            }
            return null;
        }
    }

    public static final class LightingControlFrame {
        public static final int FRAME_SIZE = 8;

        private final LightingCommand command;
        private final PatternId patternId;
        private final int red;
        private final int green;
        private final int blue;
        private final int brightness;
        private final int speed;
        private final int reserved;

        public LightingControlFrame(LightingCommand command, PatternId patternId, int red, int green, int blue, int brightness, int speed, int reserved) {
            if (red < 0 || red > 255) throw new IllegalArgumentException("Red component must be 0-255");
            if (green < 0 || green > 255) throw new IllegalArgumentException("Green component must be 0-255");
            if (blue < 0 || blue > 255) throw new IllegalArgumentException("Blue component must be 0-255");
            if (brightness < 0 || brightness > 100) throw new IllegalArgumentException("Brightness must be 0-100");
            if (speed < 1 || speed > 100) throw new IllegalArgumentException("Speed must be 1-100");

            this.command = Objects.requireNonNull(command);
            this.patternId = Objects.requireNonNull(patternId);
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.brightness = brightness;
            this.speed = speed;
            this.reserved = reserved;
        }

        public LightingCommand getCommand() { return command; }
        public PatternId getPatternId() { return patternId; }
        public int getRed() { return red; }
        public int getGreen() { return green; }
        public int getBlue() { return blue; }
        public int getBrightness() { return brightness; }
        public int getSpeed() { return speed; }
        public int getReserved() { return reserved; }

        public byte[] toByteArray() {
            return new byte[] {
                command.getId(),
                patternId.getId(),
                (byte) red,
                (byte) green,
                (byte) blue,
                (byte) brightness,
                (byte) speed,
                (byte) reserved
            };
        }

        public static LightingControlFrame fromByteArray(byte[] bytes) {
            if (bytes == null || bytes.length != FRAME_SIZE) {
                throw new IllegalArgumentException("Lighting frame must be exactly " + FRAME_SIZE + " bytes");
            }
            LightingCommand cmd = LightingCommand.fromByte(bytes[0]);
            if (cmd == null) throw new IllegalArgumentException("Invalid LightingCommand: " + bytes[0]);
            PatternId pattern = PatternId.fromByte(bytes[1]);
            if (pattern == null) throw new IllegalArgumentException("Invalid PatternId: " + bytes[1]);

            return new LightingControlFrame(
                cmd,
                pattern,
                bytes[2] & 0xFF,
                bytes[3] & 0xFF,
                bytes[4] & 0xFF,
                bytes[5] & 0xFF,
                bytes[6] & 0xFF,
                bytes[7] & 0xFF
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LightingControlFrame that = (LightingControlFrame) o;
            return red == that.red && green == that.green && blue == that.blue &&
                   brightness == that.brightness && speed == that.speed && reserved == that.reserved &&
                   command == that.command && patternId == that.patternId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(command, patternId, red, green, blue, brightness, speed, reserved);
        }
    }

    public static final class AudioControlFrame {
        public static final int FRAME_SIZE = 6;

        private final AudioCommand command;
        private final MelodyId melodyId;
        private final PlaybackState playbackState;
        private final int volume;
        private final int tempo;
        private final int reserved;

        public AudioControlFrame(AudioCommand command, MelodyId melodyId, PlaybackState playbackState, int volume, int tempo, int reserved) {
            if (volume < 0 || volume > 100) throw new IllegalArgumentException("Volume must be 0-100");
            if (tempo < 50 || tempo > 200) throw new IllegalArgumentException("Tempo must be 50-200");

            this.command = Objects.requireNonNull(command);
            this.melodyId = Objects.requireNonNull(melodyId);
            this.playbackState = Objects.requireNonNull(playbackState);
            this.volume = volume;
            this.tempo = tempo;
            this.reserved = reserved;
        }

        public AudioCommand getCommand() { return command; }
        public MelodyId getMelodyId() { return melodyId; }
        public PlaybackState getPlaybackState() { return playbackState; }
        public int getVolume() { return volume; }
        public int getTempo() { return tempo; }
        public int getReserved() { return reserved; }

        public byte[] toByteArray() {
            return new byte[] {
                command.getId(),
                melodyId.getId(),
                playbackState.getId(),
                (byte) volume,
                (byte) tempo,
                (byte) reserved
            };
        }

        public static AudioControlFrame fromByteArray(byte[] bytes) {
            if (bytes == null || bytes.length != FRAME_SIZE) {
                throw new IllegalArgumentException("Audio frame must be exactly " + FRAME_SIZE + " bytes");
            }
            AudioCommand cmd = AudioCommand.fromByte(bytes[0]);
            if (cmd == null) throw new IllegalArgumentException("Invalid AudioCommand: " + bytes[0]);
            MelodyId melody = MelodyId.fromByte(bytes[1]);
            if (melody == null) throw new IllegalArgumentException("Invalid MelodyId: " + bytes[1]);
            PlaybackState state = PlaybackState.fromByte(bytes[2]);
            if (state == null) throw new IllegalArgumentException("Invalid PlaybackState: " + bytes[2]);

            return new AudioControlFrame(
                cmd,
                melody,
                state,
                bytes[3] & 0xFF,
                bytes[4] & 0xFF,
                bytes[5] & 0xFF
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AudioControlFrame that = (AudioControlFrame) o;
            return volume == that.volume && tempo == that.tempo && reserved == that.reserved &&
                   command == that.command && melodyId == that.melodyId && playbackState == that.playbackState;
        }

        @Override
        public int hashCode() {
            return Objects.hash(command, melodyId, playbackState, volume, tempo, reserved);
        }
    }

    public static final class TelemetryFrame {
        public static final int FRAME_SIZE = 10;

        private final long systemUptimeSeconds;
        private final PatternId currentPatternId;
        private final MelodyId currentMelodyId;
        private final int currentBrightness;
        private final int currentVolume;
        private final boolean isConnected;
        private final ErrorCode errorCode;

        public TelemetryFrame(long systemUptimeSeconds, PatternId currentPatternId, MelodyId currentMelodyId, int currentBrightness, int currentVolume, boolean isConnected, ErrorCode errorCode) {
            this.systemUptimeSeconds = systemUptimeSeconds;
            this.currentPatternId = Objects.requireNonNull(currentPatternId);
            this.currentMelodyId = Objects.requireNonNull(currentMelodyId);
            this.currentBrightness = currentBrightness;
            this.currentVolume = currentVolume;
            this.isConnected = isConnected;
            this.errorCode = Objects.requireNonNull(errorCode);
        }

        public long getSystemUptimeSeconds() { return systemUptimeSeconds; }
        public PatternId getCurrentPatternId() { return currentPatternId; }
        public MelodyId getCurrentMelodyId() { return currentMelodyId; }
        public int getCurrentBrightness() { return currentBrightness; }
        public int getCurrentVolume() { return currentVolume; }
        public boolean isConnected() { return isConnected; }
        public ErrorCode getErrorCode() { return errorCode; }

        public byte[] toByteArray() {
            ByteBuffer buffer = ByteBuffer.allocate(FRAME_SIZE).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt((int) systemUptimeSeconds);
            buffer.put(currentPatternId.getId());
            buffer.put(currentMelodyId.getId());
            buffer.put((byte) currentBrightness);
            buffer.put((byte) currentVolume);
            buffer.put((byte) (isConnected ? 1 : 0));
            buffer.put(errorCode.getId());
            return buffer.array();
        }

        public static TelemetryFrame fromByteArray(byte[] bytes) {
            if (bytes == null || bytes.length != FRAME_SIZE) {
                throw new IllegalArgumentException("Telemetry frame must be exactly " + FRAME_SIZE + " bytes");
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            long uptime = Integer.toUnsignedLong(buffer.getInt());
            PatternId pattern = PatternId.fromByte(buffer.get());
            if (pattern == null) throw new IllegalArgumentException("Invalid PatternId");
            MelodyId melody = MelodyId.fromByte(buffer.get());
            if (melody == null) throw new IllegalArgumentException("Invalid MelodyId");
            int brightness = buffer.get() & 0xFF;
            int volume = buffer.get() & 0xFF;
            boolean connected = buffer.get() != 0;
            ErrorCode err = ErrorCode.fromByte(buffer.get());
            if (err == null) throw new IllegalArgumentException("Invalid ErrorCode");

            return new TelemetryFrame(uptime, pattern, melody, brightness, volume, connected, err);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TelemetryFrame that = (TelemetryFrame) o;
            return systemUptimeSeconds == that.systemUptimeSeconds && currentBrightness == that.currentBrightness &&
                   currentVolume == that.currentVolume && isConnected == that.isConnected &&
                   currentPatternId == that.currentPatternId && currentMelodyId == that.currentMelodyId &&
                   errorCode == that.errorCode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(systemUptimeSeconds, currentPatternId, currentMelodyId, currentBrightness, currentVolume, isConnected, errorCode);
        }
    }

    private ProtocolSpec() {}
}
