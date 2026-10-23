package com.xmastree.remote.protocol;

import java.util.UUID;

public final class GattProfile {
    public static final String DEVICE_NAME = "XmasTree-ESP32C6";

    public static final UUID SERVICE_UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0");
    public static final UUID LIGHTING_CONTROL_CHARACTERISTIC_UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef1");
    public static final UUID AUDIO_CONTROL_CHARACTERISTIC_UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef2");
    public static final UUID TELEMETRY_CHARACTERISTIC_UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef3");

    private GattProfile() {}
}
