package com.xmastree.remote.protocol

import java.util.UUID

object GattProfile {
    const val DEVICE_NAME = "XmasTree-ESP32C6"

    val SERVICE_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")
    val LIGHTING_CONTROL_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef1")
    val AUDIO_CONTROL_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef2")
    val TELEMETRY_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef3")
}
