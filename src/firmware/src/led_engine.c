#include <stdio.h>
#include <string.h>
#include "../include/led_engine.h"

void *xLedQueue = NULL;
static led_engine_cmd_t s_current_led_state = {
    .pattern_id = PATTERN_OFF,
    .red = 0,
    .green = 0,
    .blue = 0,
    .brightness = 50,
    .speed = 50
};

void led_engine_init(void) {
    s_current_led_state.pattern_id = PATTERN_OFF;
}

void led_engine_task(void *param) {
    (void)param;
}

bool led_engine_send_command(const led_engine_cmd_t *cmd) {
    if (cmd != NULL) {
        s_current_led_state = *cmd;
        return true;
    }
    return false;
}

void led_engine_get_current_state(led_engine_cmd_t *out_state) {
    if (out_state != NULL) {
        *out_state = s_current_led_state;
    }
}
