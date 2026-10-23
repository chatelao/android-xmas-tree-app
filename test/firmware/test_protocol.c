#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "../../src/firmware/include/gatt_profile.h"
#include "../../src/firmware/include/protocol_spec.h"

void test_gatt_profile_uuids(void) {
    printf("Testing GATT Profile UUID definitions...\n");
    assert(strcmp(BLE_DEVICE_NAME, "XmasTree-ESP32C6") == 0);
    assert(strcmp(SERVICE_UUID_STR, "12345678-1234-5678-1234-56789abcdef0") == 0);
    assert(strcmp(CHARACTERISTIC_LIGHTING_UUID_STR, "12345678-1234-5678-1234-56789abcdef1") == 0);
    assert(strcmp(CHARACTERISTIC_AUDIO_UUID_STR, "12345678-1234-5678-1234-56789abcdef2") == 0);
    assert(strcmp(CHARACTERISTIC_TELEMETRY_UUID_STR, "12345678-1234-5678-1234-56789abcdef3") == 0);
    printf("  PASSED: GATT Profile UUID tests\n");
}

void test_lighting_control_frame(void) {
    printf("Testing Lighting Control Frame parser...\n");
    uint8_t valid_bytes[8] = { 0x01, 0x02, 0xFF, 0x00, 0x80, 0x32, 0x4B, 0x00 }; // SET_PATTERN, RAINBOW_WAVE, R=255, G=0, B=128, Brightness=50%, Speed=75
    lighting_control_payload_t payload;

    protocol_status_t status = parse_lighting_control_frame(valid_bytes, 8, &payload);
    assert(status == PROTOCOL_OK);
    assert(payload.command_id == CMD_LIGHTING_SET_PATTERN);
    assert(payload.pattern_id == PATTERN_RAINBOW_WAVE);
    assert(payload.red == 255);
    assert(payload.green == 0);
    assert(payload.blue == 128);
    assert(payload.brightness == 50);
    assert(payload.speed == 75);

    // Test size error
    assert(parse_lighting_control_frame(valid_bytes, 7, &payload) == PROTOCOL_ERROR_INVALID_SIZE);

    // Test invalid command
    valid_bytes[0] = 0x99;
    assert(parse_lighting_control_frame(valid_bytes, 8, &payload) == PROTOCOL_ERROR_INVALID_CMD);

    // Test invalid brightness (>100)
    valid_bytes[0] = 0x01;
    valid_bytes[5] = 101;
    assert(parse_lighting_control_frame(valid_bytes, 8, &payload) == PROTOCOL_ERROR_INVALID_PARAM);

    printf("  PASSED: Lighting Control Frame tests\n");
}

void test_audio_control_frame(void) {
    printf("Testing Audio Control Frame parser...\n");
    uint8_t valid_bytes[6] = { 0x10, 0x01, 0x01, 0x50, 0x64, 0x00 }; // PLAY_SONG, JINGLE_BELLS, PLAY, Volume=80%, Tempo=100
    audio_control_payload_t payload;

    protocol_status_t status = parse_audio_control_frame(valid_bytes, 6, &payload);
    assert(status == PROTOCOL_OK);
    assert(payload.command_id == CMD_AUDIO_PLAY_SONG);
    assert(payload.melody_id == MELODY_JINGLE_BELLS);
    assert(payload.playback_state == AUDIO_STATE_PLAY);
    assert(payload.volume == 80);
    assert(payload.tempo == 100);

    // Test size error
    assert(parse_audio_control_frame(valid_bytes, 5, &payload) == PROTOCOL_ERROR_INVALID_SIZE);

    // Test invalid melody
    valid_bytes[1] = 0xFF;
    assert(parse_audio_control_frame(valid_bytes, 6, &payload) == PROTOCOL_ERROR_INVALID_PARAM);

    printf("  PASSED: Audio Control Frame tests\n");
}

void test_telemetry_frame(void) {
    printf("Testing Telemetry Frame serializer & parser...\n");
    telemetry_payload_t orig_payload = {
        .system_uptime = 0x12345678,
        .current_pattern = PATTERN_SPARKLE,
        .current_melody = MELODY_SILENT_NIGHT,
        .current_brightness = 90,
        .current_volume = 40,
        .connection_state = 1,
        .error_code = ERROR_CODE_OK
    };

    uint8_t buffer[10];
    protocol_status_t status = serialize_telemetry_frame(&orig_payload, buffer, sizeof(buffer));
    assert(status == PROTOCOL_OK);

    // Check Little-Endian uptime serialization
    assert(buffer[0] == 0x78);
    assert(buffer[1] == 0x56);
    assert(buffer[2] == 0x34);
    assert(buffer[3] == 0x12);
    assert(buffer[4] == PATTERN_SPARKLE);
    assert(buffer[5] == MELODY_SILENT_NIGHT);
    assert(buffer[6] == 90);
    assert(buffer[7] == 40);
    assert(buffer[8] == 1);
    assert(buffer[9] == ERROR_CODE_OK);

    telemetry_payload_t parsed_payload;
    status = parse_telemetry_frame(buffer, sizeof(buffer), &parsed_payload);
    assert(status == PROTOCOL_OK);
    assert(parsed_payload.system_uptime == 0x12345678);
    assert(parsed_payload.current_pattern == PATTERN_SPARKLE);
    assert(parsed_payload.current_melody == MELODY_SILENT_NIGHT);
    assert(parsed_payload.current_brightness == 90);
    assert(parsed_payload.current_volume == 40);
    assert(parsed_payload.connection_state == 1);
    assert(parsed_payload.error_code == ERROR_CODE_OK);

    printf("  PASSED: Telemetry Frame tests\n");
}

int main(void) {
    printf("==========================================\n");
    printf("Running Firmware Protocol Unit Tests\n");
    printf("==========================================\n");
    test_gatt_profile_uuids();
    test_lighting_control_frame();
    test_audio_control_frame();
    test_telemetry_frame();
    printf("==========================================\n");
    printf("All Firmware Protocol Tests Passed Successfully!\n");
    printf("==========================================\n");
    return 0;
}
