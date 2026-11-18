#include <stdio.h>
#include <string.h>
#include "../include/ble_manager.h"
#include "../include/led_engine.h"
#include "../include/audio_engine.h"

#ifdef ESP_PLATFORM
#include "host/ble_hs.h"
#include "host/ble_gatt.h"
#include "os/os_mbuf.h"
#endif

static ble_manager_state_t s_ble_state = BLE_STATE_DISCONNECTED;
static ble_lighting_cmd_cb_t s_lighting_cb = NULL;
static ble_audio_cmd_cb_t s_audio_cb = NULL;
static ble_telemetry_notify_cb_t s_telemetry_cb = NULL;

static bool s_telemetry_subscribed = false;
static system_error_code_t s_error_code = ERROR_CODE_OK;
static uint32_t s_last_telemetry_ms = 0;
static bool s_has_sent_telemetry = false;
static uint8_t s_last_telemetry_frame[TELEMETRY_PAYLOAD_SIZE] = {0};

#ifdef ESP_PLATFORM
static uint16_t s_conn_handle = 0;
static uint16_t s_telemetry_val_handle = 0;
#endif

void nimble_host_task(void *param) {
    (void)param;
    s_ble_state = BLE_STATE_ADVERTISING;
}

void cmd_dispatcher_task(void *param) {
    (void)param;
}

void ble_manager_init(void) {
    s_ble_state = BLE_STATE_DISCONNECTED;
    s_telemetry_subscribed = false;
    s_error_code = ERROR_CODE_OK;
    s_last_telemetry_ms = 0;
    s_has_sent_telemetry = false;
    memset(s_last_telemetry_frame, 0, sizeof(s_last_telemetry_frame));
}

void ble_manager_set_state(ble_manager_state_t state) {
    s_ble_state = state;
    if (state != BLE_STATE_CONNECTED) {
        s_telemetry_subscribed = false;
        s_has_sent_telemetry = false;
    }
}

ble_manager_state_t ble_manager_get_state(void) {
    return s_ble_state;
}

void ble_manager_set_telemetry_subscribed(bool subscribed) {
    s_telemetry_subscribed = subscribed;
    if (!subscribed) {
        s_has_sent_telemetry = false;
    }
}

bool ble_manager_is_telemetry_subscribed(void) {
    return s_telemetry_subscribed;
}

void ble_manager_set_error_code(system_error_code_t err) {
    s_error_code = err;
}

system_error_code_t ble_manager_get_error_code(void) {
    return s_error_code;
}

bool ble_manager_build_telemetry_snapshot(uint32_t uptime_sec, telemetry_payload_t *out_telemetry) {
    if (out_telemetry == NULL) {
        return false;
    }

    led_engine_cmd_t led_state = {0};
    audio_engine_cmd_t audio_state = {0};
    led_engine_get_current_state(&led_state);
    audio_engine_get_current_state(&audio_state);

    out_telemetry->system_uptime = uptime_sec;
    out_telemetry->current_pattern = (uint8_t)led_state.pattern_id;
    out_telemetry->current_melody = (uint8_t)audio_state.melody_id;
    out_telemetry->current_brightness = led_state.brightness;
    out_telemetry->current_volume = audio_state.volume;
    out_telemetry->connection_state = (s_ble_state == BLE_STATE_CONNECTED) ? 1 : 0;
    out_telemetry->error_code = (uint8_t)s_error_code;
    return true;
}

bool ble_manager_send_telemetry(const telemetry_payload_t *telemetry) {
    if (telemetry == NULL || s_ble_state != BLE_STATE_CONNECTED || !s_telemetry_subscribed) {
        return false;
    }

    uint8_t frame[TELEMETRY_PAYLOAD_SIZE] = {0};
    if (serialize_telemetry_frame(telemetry, frame, sizeof(frame)) != PROTOCOL_OK) {
        return false;
    }

    memcpy(s_last_telemetry_frame, frame, TELEMETRY_PAYLOAD_SIZE);

    if (s_telemetry_cb != NULL) {
        s_telemetry_cb(frame, TELEMETRY_PAYLOAD_SIZE);
    }

#ifdef ESP_PLATFORM
    if (s_telemetry_val_handle != 0) {
        struct os_mbuf *om = ble_hs_mbuf_from_flat(frame, TELEMETRY_PAYLOAD_SIZE);
        if (om != NULL) {
            if (ble_gatts_notify_custom(s_conn_handle, s_telemetry_val_handle, om) != 0) {
                return false;
            }
        }
    }
#endif

    return true;
}

bool ble_manager_telemetry_tick(uint32_t now_ms) {
    if (s_ble_state != BLE_STATE_CONNECTED || !s_telemetry_subscribed) {
        return false;
    }

    if (s_has_sent_telemetry && (uint32_t)(now_ms - s_last_telemetry_ms) < BLE_TELEMETRY_INTERVAL_MS) {
        return false;
    }

    telemetry_payload_t snapshot = {0};
    if (!ble_manager_build_telemetry_snapshot(now_ms / 1000U, &snapshot)) {
        return false;
    }

    if (!ble_manager_send_telemetry(&snapshot)) {
        return false;
    }

    s_last_telemetry_ms = now_ms;
    s_has_sent_telemetry = true;
    return true;
}

bool ble_manager_get_last_telemetry_frame(uint8_t *out_buf, size_t buf_size) {
    if (out_buf == NULL || buf_size < TELEMETRY_PAYLOAD_SIZE) {
        return false;
    }
    memcpy(out_buf, s_last_telemetry_frame, TELEMETRY_PAYLOAD_SIZE);
    return true;
}

void ble_manager_register_callbacks(ble_lighting_cmd_cb_t lighting_cb, ble_audio_cmd_cb_t audio_cb) {
    s_lighting_cb = lighting_cb;
    s_audio_cb = audio_cb;
}

void ble_manager_register_telemetry_callback(ble_telemetry_notify_cb_t telemetry_cb) {
    s_telemetry_cb = telemetry_cb;
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
