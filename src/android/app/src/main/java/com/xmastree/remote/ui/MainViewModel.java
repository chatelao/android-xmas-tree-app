package com.xmastree.remote.ui;

import com.xmastree.remote.ble.BleManager;
import com.xmastree.remote.data.Preset;
import com.xmastree.remote.protocol.ProtocolSpec.MelodyId;
import com.xmastree.remote.protocol.ProtocolSpec.PatternId;
import java.util.ArrayList;
import java.util.List;

public class MainViewModel {
    public static class UiState {
        private BleManager.ConnectionState connectionState = BleManager.ConnectionState.DISCONNECTED;
        private PatternId selectedPattern = PatternId.OFF;
        private int red = 255;
        private int green = 0;
        private int blue = 0;
        private int brightness = 80;
        private int speed = 50;
        private MelodyId selectedMelody = MelodyId.SILENT;
        private int volume = 50;
        private int tempo = 100;
        private List<Preset> savedPresets = new ArrayList<>();

        public BleManager.ConnectionState getConnectionState() { return connectionState; }
        public PatternId getSelectedPattern() { return selectedPattern; }
        public int getRed() { return red; }
        public int getGreen() { return green; }
        public int getBlue() { return blue; }
        public int getBrightness() { return brightness; }
        public int getSpeed() { return speed; }
        public MelodyId getSelectedMelody() { return selectedMelody; }
        public int getVolume() { return volume; }
        public int getTempo() { return tempo; }
        public List<Preset> getSavedPresets() { return savedPresets; }

        public void setConnectionState(BleManager.ConnectionState state) { this.connectionState = state; }
        public void setSelectedPattern(PatternId pattern) { this.selectedPattern = pattern; }
        public void setColor(int r, int g, int b) { this.red = r; this.green = g; this.blue = b; }
        public void setBrightness(int brightness) { this.brightness = brightness; }
        public void setSpeed(int speed) { this.speed = speed; }
        public void setSelectedMelody(MelodyId melody) { this.selectedMelody = melody; }
        public void setVolume(int volume) { this.volume = volume; }
        public void setTempo(int tempo) { this.tempo = tempo; }
        public void addPreset(Preset preset) { this.savedPresets.add(preset); }
    }

    private final UiState uiState = new UiState();

    public UiState getUiState() {
        return uiState;
    }

    public void connectDevice(String address) {
        uiState.setConnectionState(BleManager.ConnectionState.CONNECTED);
    }

    public void disconnectDevice() {
        uiState.setConnectionState(BleManager.ConnectionState.DISCONNECTED);
    }

    public void applyPreset(Preset preset) {
        if (preset != null) {
            uiState.setSelectedPattern(preset.getPatternId());
            uiState.setColor(preset.getRed(), preset.getGreen(), preset.getBlue());
            uiState.setBrightness(preset.getBrightness());
            uiState.setSpeed(preset.getSpeed());
            uiState.setSelectedMelody(preset.getMelodyId());
            uiState.setVolume(preset.getVolume());
            uiState.setTempo(preset.getTempo());
        }
    }
}
