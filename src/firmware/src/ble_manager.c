#include <stdio.h>
#include <string.h>
#include "../include/ble_manager.h"
#include "../include/led_engine.h"
#include "../include/audio_engine.h"

static ble_manager_state_t s_ble_state = BLE_STATE_DISCONNECTED;
static ble_lighting_cmd_cb_t s_lighting_cb = NULL;
static ble_audio_cmd_cb_t s_audio_cb = NULL;

void nimble_host_task(void *param) {
    (void)param;
    s_ble_state = BLE_STATE_ADVERTISING;
}

void cmd_dispatcher_task(void *param) {
    (void)param;
}

void ble_manager_init(void) {
    s_ble_state = BLE_STATE_DISCONNECTED;
}

ble_manager_state_t ble_manager_get_state(void) {
    return s_ble_state;
}

bool ble_manager_send_telemetry(const telemetry_payload_t *telemetry) {
    return telemetry != NULL && s_ble_state == BLE_STATE_CONNECTED;
}

void ble_manager_register_callbacks(ble_lighting_cmd_cb_t lighting_cb, ble_audio_cmd_cb_t audio_cb) {
    s_lighting_cb = lighting_cb;
    s_audio_cb = audio_cb;
}

bool ble_manager_process_lighting_cmd(const uint8_t *buffer, size_t size) {
    lighting_control_payload_t payload;
    if (parse_lighting_control_frame(buffer, size, &payload) != PROTOCOL_OK) {
        return false;
    }

    if (s_lighting_cb != NULL) {
        s_lighting_cb(&payload);
    }

    led_engine_cmd_t led_cmd = {
        .pattern_id = payload.pattern_id,
        .red = payload.red,
        .green = payload.green,
        .blue = payload.blue,
        .brightness = payload.brightness,
        .speed = payload.speed
    };
    led_engine_send_command(&led_cmd);
    return true;
}

bool ble_manager_process_audio_cmd(const uint8_t *buffer, size_t size) {
    audio_control_payload_t payload;
    if (parse_audio_control_frame(buffer, size, &payload) != PROTOCOL_OK) {
        return false;
    }

    if (s_audio_cb != NULL) {
        s_audio_cb(&payload);
    }

    audio_engine_cmd_t audio_cmd = {
        .melody_id = payload.melody_id,
        .playback_state = payload.playback_state,
        .volume = payload.volume,
        .tempo = payload.tempo
    };
    audio_engine_send_command(&audio_cmd);
    return true;
}
