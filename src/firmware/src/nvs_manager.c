#include <stdio.h>
#include <string.h>
#include "../include/nvs_manager.h"

static nvs_saved_state_t s_nvs_state = {
    .pattern_id = 0,
    .red = 255,
    .green = 255,
    .blue = 255,
    .brightness = 80,
    .speed = 50,
    .melody_id = 0,
    .volume = 50,
    .tempo = 100
};

void nvs_manager_init(void) {
}

bool nvs_save_system_state(const nvs_saved_state_t *state) {
    if (state != NULL) {
        s_nvs_state = *state;
        return true;
    }
    return false;
}

bool nvs_load_system_state(nvs_saved_state_t *out_state) {
    if (out_state != NULL) {
        *out_state = s_nvs_state;
        return true;
    }
    return false;
}
