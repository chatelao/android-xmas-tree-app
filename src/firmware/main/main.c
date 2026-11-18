#include <stdio.h>
#include "../include/protocol_spec.h"
#include "../include/led_engine.h"
#include "../include/audio_engine.h"
#include "../include/ble_manager.h"
#include "../include/nvs_manager.h"

#ifdef ESP_PLATFORM
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_timer.h"
#endif

static void on_lighting_command(const lighting_control_payload_t *payload) {
    if (payload == NULL) {
        return;
    }
    led_engine_cmd_t cmd = {
        .pattern_id = payload->pattern_id,
        .red = payload->red,
        .green = payload->green,
        .blue = payload->blue,
        .brightness = payload->brightness,
        .speed = payload->speed
    };
    led_engine_send_command(&cmd);
}

static void on_audio_command(const audio_control_payload_t *payload) {
    if (payload == NULL) {
        return;
    }
    audio_engine_cmd_t cmd = {
        .melody_id = payload->melody_id,
        .playback_state = payload->playback_state,
        .volume = payload->volume,
        .tempo = payload->tempo
    };
    audio_engine_send_command(&cmd);
}

void app_main(void) {
    nvs_manager_init();
    led_engine_init();
    audio_engine_init();
    ble_manager_init();

    nvs_apply_saved_state_to_engines();
    ble_manager_register_callbacks(on_lighting_command, on_audio_command);

#ifdef ESP_PLATFORM
    while (1) {
        uint32_t now_ms = (uint32_t)(esp_timer_get_time() / 1000ULL);
        ble_manager_telemetry_tick(now_ms);
        audio_engine_playback_step();
        vTaskDelay(pdMS_TO_TICKS(50));
    }
#endif
}
