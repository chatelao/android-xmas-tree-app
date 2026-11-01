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

typedef struct {
    uint8_t r;
    uint8_t g;
    uint8_t b;
} rgb_color_t;

typedef struct {
    uint8_t gpio_pin;
    uint32_t clock_hz;
    uint16_t t0h_ns;
    uint16_t t0l_ns;
    uint16_t t1h_ns;
    uint16_t t1l_ns;
    uint32_t reset_ns;
} ws2812b_rmt_config_t;

// Queue handle declaration stub
extern void *xLedQueue;

void led_engine_task(void *param);
void led_engine_init(void);
bool led_engine_send_command(const led_engine_cmd_t *cmd);
void led_engine_get_current_state(led_engine_cmd_t *out_state);

// WS2812B RMT peripheral timing configuration helper
ws2812b_rmt_config_t led_engine_get_rmt_timing(void);

// Frame rendering algorithm engine for Neopixel strip
bool led_engine_render_frame(const led_engine_cmd_t *cmd, uint32_t frame_index, rgb_color_t *out_pixels, uint16_t num_leds);

#endif // LED_ENGINE_H
