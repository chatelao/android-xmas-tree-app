#include <stdio.h>
#include <string.h>
#include "../include/nvs_manager.h"
#include "../include/led_engine.h"
#include "../include/audio_engine.h"
#include "../include/ble_manager.h"

#ifdef ESP_PLATFORM
#include "nvs_flash.h"
#include "nvs.h"
#endif

static const nvs_saved_state_t s_factory_defaults = {
    .pattern_id = PATTERN_OFF,
    .red = 255,
    .green = 255,
    .blue = 255,
    .brightness = 80,
    .speed = 50,
    .melody_id = MELODY_SILENT,
    .volume = 50,
    .tempo = 100
};

static nvs_saved_state_t s_nvs_state = {
    .pattern_id = PATTERN_OFF,
    .red = 255,
    .green = 255,
    .blue = 255,
    .brightness = 80,
    .speed = 50,
    .melody_id = MELODY_SILENT,
    .volume = 50,
    .tempo = 100
};

static bool s_nvs_initialized = false;

static nvs_saved_state_t clamp_state(const nvs_saved_state_t *in) {
    nvs_saved_state_t out = *in;
    if (out.brightness > 100) {
        out.brightness = 100;
    }
    if (out.speed == 0) {
        out.speed = 1;
    } else if (out.speed > 100) {
        out.speed = 100;
    }
    if (out.volume > 100) {
        out.volume = 100;
    }
    if (out.tempo < 50) {
        out.tempo = 50;
    } else if (out.tempo > 200) {
        out.tempo = 200;
    }
    return out;
}

void nvs_manager_init(void) {
#ifdef ESP_PLATFORM
    esp_err_t err = nvs_flash_init();
    if (err == ESP_ERR_NVS_NO_FREE_PAGES || err == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        if (nvs_flash_erase() == ESP_OK) {
            err = nvs_flash_init();
        }
    }
    if (err != ESP_OK) {
        s_nvs_initialized = false;
        ble_manager_set_error_code(ERROR_CODE_NVS_ERROR);
        return;
    }
#endif
    s_nvs_initialized = true;
}

bool nvs_manager_is_initialized(void) {
    return s_nvs_initialized;
}

bool nvs_save_system_state(const nvs_saved_state_t *state) {
    if (state == NULL) {
        return false;
    }

    if (!s_nvs_initialized) {
        nvs_manager_init();
        if (!s_nvs_initialized) {
            return false;
        }
    }

    nvs_saved_state_t sanitized = clamp_state(state);
    s_nvs_state = sanitized;

#ifdef ESP_PLATFORM
    nvs_handle_t handle;
    esp_err_t err = nvs_open(NVS_NAMESPACE_NAME, NVS_READWRITE, &handle);
    if (err != ESP_OK) {
        ble_manager_set_error_code(ERROR_CODE_NVS_ERROR);
        return false;
    }

    bool ok = true;
    ok &= (nvs_set_u8(handle, "pattern_id", sanitized.pattern_id) == ESP_OK);
    ok &= (nvs_set_u8(handle, "red", sanitized.red) == ESP_OK);
    ok &= (nvs_set_u8(handle, "green", sanitized.green) == ESP_OK);
    ok &= (nvs_set_u8(handle, "blue", sanitized.blue) == ESP_OK);
    ok &= (nvs_set_u8(handle, "brightness", sanitized.brightness) == ESP_OK);
    ok &= (nvs_set_u8(handle, "speed", sanitized.speed) == ESP_OK);
    ok &= (nvs_set_u8(handle, "melody_id", sanitized.melody_id) == ESP_OK);
    ok &= (nvs_set_u8(handle, "volume", sanitized.volume) == ESP_OK);
    ok &= (nvs_set_u8(handle, "tempo", sanitized.tempo) == ESP_OK);

    if (ok) {
        ok &= (nvs_commit(handle) == ESP_OK);
    }
    nvs_close(handle);

    if (!ok) {
        ble_manager_set_error_code(ERROR_CODE_NVS_ERROR);
        return false;
    }
#endif

    return true;
}

bool nvs_load_system_state(nvs_saved_state_t *out_state) {
    if (out_state == NULL) {
        return false;
    }

    if (!s_nvs_initialized) {
        nvs_manager_init();
        if (!s_nvs_initialized) {
            return false;
        }
    }

#ifdef ESP_PLATFORM
    nvs_handle_t handle;
    esp_err_t err = nvs_open(NVS_NAMESPACE_NAME, NVS_READONLY, &handle);
    if (err == ESP_OK) {
        nvs_saved_state_t loaded = s_factory_defaults;
        (void)nvs_get_u8(handle, "pattern_id", &loaded.pattern_id);
        (void)nvs_get_u8(handle, "red", &loaded.red);
        (void)nvs_get_u8(handle, "green", &loaded.green);
        (void)nvs_get_u8(handle, "blue", &loaded.blue);
        (void)nvs_get_u8(handle, "brightness", &loaded.brightness);
        (void)nvs_get_u8(handle, "speed", &loaded.speed);
        (void)nvs_get_u8(handle, "melody_id", &loaded.melody_id);
        (void)nvs_get_u8(handle, "volume", &loaded.volume);
        (void)nvs_get_u8(handle, "tempo", &loaded.tempo);
        nvs_close(handle);
        s_nvs_state = clamp_state(&loaded);
    }
#endif

    *out_state = s_nvs_state;
    return true;
}

bool nvs_apply_saved_state_to_engines(void) {
    nvs_saved_state_t saved = {0};
    if (!nvs_load_system_state(&saved)) {
        return false;
    }

    led_engine_cmd_t led_cmd = {
        .pattern_id = (pattern_id_t)saved.pattern_id,
        .red = saved.red,
        .green = saved.green,
        .blue = saved.blue,
        .brightness = saved.brightness,
        .speed = saved.speed
    };
    if (!led_engine_send_command(&led_cmd)) {
        return false;
    }

    audio_engine_cmd_t audio_cmd = {
        .melody_id = (melody_id_t)saved.melody_id,
        .playback_state = AUDIO_STATE_STOP, // Keep audio silent on boot until user presses Play
        .volume = saved.volume,
        .tempo = saved.tempo
    };
    if (!audio_engine_send_command(&audio_cmd)) {
        return false;
    }

    return true;
}

bool nvs_save_current_engine_state(void) {
    led_engine_cmd_t led_state = {0};
    audio_engine_cmd_t audio_state = {0};
    led_engine_get_current_state(&led_state);
    audio_engine_get_current_state(&audio_state);

    nvs_saved_state_t to_save = {
        .pattern_id = (uint8_t)led_state.pattern_id,
        .red = led_state.red,
        .green = led_state.green,
        .blue = led_state.blue,
        .brightness = led_state.brightness,
        .speed = led_state.speed,
        .melody_id = (uint8_t)audio_state.melody_id,
        .volume = audio_state.volume,
        .tempo = audio_state.tempo
    };

    return nvs_save_system_state(&to_save);
}

void nvs_reset_defaults(void) {
    s_nvs_state = s_factory_defaults;
    if (s_nvs_initialized) {
        (void)nvs_save_system_state(&s_factory_defaults);
    }
}
