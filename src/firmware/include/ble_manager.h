#ifndef BLE_MANAGER_H
#define BLE_MANAGER_H

#include <stdbool.h>
#include <stdint.h>
#include "protocol_spec.h"

typedef enum {
    BLE_STATE_DISCONNECTED = 0,
    BLE_STATE_ADVERTISING  = 1,
    BLE_STATE_CONNECTED    = 2
} ble_manager_state_t;

typedef void (*ble_lighting_cmd_cb_t)(const lighting_control_payload_t *payload);
typedef void (*ble_audio_cmd_cb_t)(const audio_control_payload_t *payload);

void nimble_host_task(void *param);
void cmd_dispatcher_task(void *param);

void ble_manager_init(void);
ble_manager_state_t ble_manager_get_state(void);
bool ble_manager_send_telemetry(const telemetry_payload_t *telemetry);
void ble_manager_register_callbacks(ble_lighting_cmd_cb_t lighting_cb, ble_audio_cmd_cb_t audio_cb);

#endif // BLE_MANAGER_H
