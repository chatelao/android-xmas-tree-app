#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "../../src/firmware/include/ble_manager.h"
#include "../../src/firmware/include/led_engine.h"
#include "../../src/firmware/include/audio_engine.h"
#include "../../src/firmware/include/nvs_manager.h"

void test_ble_manager_stub(void) {
    printf("Testing BLE Manager stub...\n");
    ble_manager_init();
    assert(ble_manager_get_state() == BLE_STATE_DISCONNECTED);

    nimble_host_task(NULL);
    assert(ble_manager_get_state() == BLE_STATE_ADVERTISING);

    telemetry_payload_t telemetry = {0};
    assert(ble_manager_send_telemetry(&telemetry) == false); // Not connected
    printf("  PASSED: BLE Manager stub tests\n");
}

void test_led_engine_stub(void) {
    printf("Testing LED Engine stub...\n");
    led_engine_init();
    led_engine_cmd_t cmd = {
        .pattern_id = PATTERN_RAINBOW_WAVE,
        .red = 255,
        .green = 128,
        .blue = 0,
        .brightness = 75,
        .speed = 80
    };

    assert(led_engine_send_command(&cmd) == true);

    led_engine_cmd_t state = {0};
    led_engine_get_current_state(&state);
    assert(state.pattern_id == PATTERN_RAINBOW_WAVE);
    assert(state.red == 255);
    assert(state.green == 128);
    assert(state.blue == 0);
    assert(state.brightness == 75);
    assert(state.speed == 80);

    printf("  PASSED: LED Engine stub tests\n");
}

void test_audio_engine_stub(void) {
    printf("Testing Audio Engine stub...\n");
    audio_engine_init();
    audio_engine_cmd_t cmd = {
        .melody_id = MELODY_JINGLE_BELLS,
        .playback_state = AUDIO_STATE_PLAY,
        .volume = 85,
        .tempo = 105
    };

    assert(audio_engine_send_command(&cmd) == true);

    audio_engine_cmd_t state = {0};
    audio_engine_get_current_state(&state);
    assert(state.melody_id == MELODY_JINGLE_BELLS);
    assert(state.playback_state == AUDIO_STATE_PLAY);
    assert(state.volume == 85);
    assert(state.tempo == 105);

    printf("  PASSED: Audio Engine stub tests\n");
}

void test_nvs_manager_stub(void) {
    printf("Testing NVS Manager stub...\n");
    nvs_manager_init();

    nvs_saved_state_t save_state = {
        .pattern_id = PATTERN_SPARKLE,
        .red = 100,
        .green = 200,
        .blue = 255,
        .brightness = 95,
        .speed = 60,
        .melody_id = MELODY_SILENT_NIGHT,
        .volume = 40,
        .tempo = 90
    };

    assert(nvs_save_system_state(&save_state) == true);

    nvs_saved_state_t loaded_state = {0};
    assert(nvs_load_system_state(&loaded_state) == true);
    assert(loaded_state.pattern_id == PATTERN_SPARKLE);
    assert(loaded_state.melody_id == MELODY_SILENT_NIGHT);
    assert(loaded_state.brightness == 95);
    assert(loaded_state.volume == 40);

    printf("  PASSED: NVS Manager stub tests\n");
}

int main(void) {
    printf("==========================================\n");
    printf("Running Firmware Architecture Unit Tests\n");
    printf("==========================================\n");

    test_ble_manager_stub();
    test_led_engine_stub();
    test_audio_engine_stub();
    test_nvs_manager_stub();

    printf("==========================================\n");
    printf("All Firmware Architecture Tests Passed Successfully!\n");
    printf("==========================================\n");
    return 0;
}
