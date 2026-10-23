#ifndef LED_ENGINE_H
#define LED_ENGINE_H

#include <stdbool.h>
#include <stdint.h>
#include "protocol_spec.h"

#define LED_STRIP_GPIO_PIN 8
#define LED_STRIP_NUM_LEDS 30

typedef struct {
    pattern_id_t pattern_id;
    uint8_t red;
    uint8_t green;
    uint8_t blue;
    uint8_t brightness;
    uint8_t speed;
} led_engine_cmd_t;

// Queue handle declaration stub
extern void *xLedQueue;

void led_engine_task(void *param);
void led_engine_init(void);
bool led_engine_send_command(const led_engine_cmd_t *cmd);
void led_engine_get_current_state(led_engine_cmd_t *out_state);

#endif // LED_ENGINE_H
