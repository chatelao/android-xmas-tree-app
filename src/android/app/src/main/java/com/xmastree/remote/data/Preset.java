package com.xmastree.remote.data;

import com.xmastree.remote.protocol.ProtocolSpec.MelodyId;
import com.xmastree.remote.protocol.ProtocolSpec.PatternId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Preset {
    private final String id;
    private final String name;
    private final PatternId patternId;
    private final int red;
    private final int green;
    private final int blue;
    private final int brightness;
    private final int speed;
    private final MelodyId melodyId;
    private final int volume;
    private final int tempo;

    public Preset(String id, String name, PatternId patternId, int red, int green, int blue,
                  int brightness, int speed, MelodyId melodyId, int volume, int tempo) {
        this.id = id;
        this.name = name;
        this.patternId = patternId;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.brightness = brightness;
        this.speed = speed;
        this.melodyId = melodyId;
        this.volume = volume;
        this.tempo = tempo;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public PatternId getPatternId() { return patternId; }
    public int getRed() { return red; }
    public int getGreen() { return green; }
    public int getBlue() { return blue; }
    public int getBrightness() { return brightness; }
    public int getSpeed() { return speed; }
    public MelodyId getMelodyId() { return melodyId; }
    public int getVolume() { return volume; }
    public int getTempo() { return tempo; }

    public static class Repository {
        private final Map<String, Preset> presets = new HashMap<>();

        public List<Preset> getAllPresets() {
            return new ArrayList<>(presets.values());
        }

        public Preset getPresetById(String id) {
            return presets.get(id);
        }

        public boolean savePreset(Preset preset) {
            if (preset != null && preset.getId() != null) {
                presets.put(preset.getId(), preset);
                return true;
            }
            return false;
        }

        public boolean deletePreset(String id) {
            return presets.remove(id) != null;
        }
    }
}
