#ifndef NVS_MANAGER_H
#define NVS_MANAGER_H

#include <stdbool.h>
#include <stdint.h>
#include "protocol_spec.h"

typedef struct {
    uint8_t pattern_id;
    uint8_t red;
    uint8_t green;
    uint8_t blue;
    uint8_t brightness;
    uint8_t speed;
    uint8_t melody_id;
    uint8_t volume;
    uint8_t tempo;
} nvs_saved_state_t;

void nvs_manager_init(void);
bool nvs_save_system_state(const nvs_saved_state_t *state);
bool nvs_load_system_state(nvs_saved_state_t *out_state);

#endif // NVS_MANAGER_H
