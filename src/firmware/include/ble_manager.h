#ifndef BLE_MANAGER_H
#define BLE_MANAGER_H

#include <stdbool.h>
#include <stdint.h>
#include <stddef.h>
#include "protocol_spec.h"

#define BLE_TELEMETRY_INTERVAL_MS 1000U

typedef enum {
    BLE_STATE_DISCONNECTED = 0,
    BLE_STATE_ADVERTISING  = 1,
    BLE_STATE_CONNECTED    = 2
} ble_manager_state_t;

typedef void (*ble_lighting_cmd_cb_t)(const lighting_control_payload_t *payload);
typedef void (*ble_audio_cmd_cb_t)(const audio_control_payload_t *payload);
typedef void (*ble_telemetry_notify_cb_t)(const uint8_t *frame, size_t size);

void nimble_host_task(void *param);
void cmd_dispatcher_task(void *param);

void ble_manager_init(void);
void ble_manager_set_state(ble_manager_state_t state);
ble_manager_state_t ble_manager_get_state(void);

void ble_manager_set_telemetry_subscribed(bool subscribed);
bool ble_manager_is_telemetry_subscribed(void);
void ble_manager_set_error_code(system_error_code_t err);
system_error_code_t ble_manager_get_error_code(void);

bool ble_manager_build_telemetry_snapshot(uint32_t uptime_sec, telemetry_payload_t *out_telemetry);
bool ble_manager_send_telemetry(const telemetry_payload_t *telemetry);
bool ble_manager_telemetry_tick(uint32_t now_ms);
bool ble_manager_get_last_telemetry_frame(uint8_t *out_buf, size_t buf_size);

void ble_manager_register_callbacks(ble_lighting_cmd_cb_t lighting_cb, ble_audio_cmd_cb_t audio_cb);
void ble_manager_register_telemetry_callback(ble_telemetry_notify_cb_t telemetry_cb);

bool ble_manager_process_lighting_cmd(const uint8_t *buffer, size_t size);
bool ble_manager_process_audio_cmd(const uint8_t *buffer, size_t size);

#endif // BLE_MANAGER_H
