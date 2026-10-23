#include <stdio.h>
#include <string.h>
#include "../include/ble_manager.h"

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
