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
    s_current_led_state.red = 0;
    s_current_led_state.green = 0;
    s_current_led_state.blue = 0;
    s_current_led_state.brightness = 50;
    s_current_led_state.speed = 50;
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

ws2812b_rmt_config_t led_engine_get_rmt_timing(void) {
    ws2812b_rmt_config_t config = {
        .gpio_pin = LED_STRIP_GPIO_PIN,
        .clock_hz = 10000000UL, // 10 MHz resolution (100 ns per tick)
        .t0h_ns = 350,          // T0H: 350 ns (3.5 ticks)
        .t0l_ns = 800,          // T0L: 800 ns (8 ticks)
        .t1h_ns = 700,          // T1H: 700 ns (7 ticks)
        .t1l_ns = 600,          // T1L: 600 ns (6 ticks)
        .reset_ns = 280000UL    // Reset: 280 us
    };
    return config;
}

static void hsv_to_rgb(uint8_t hue, uint8_t val, uint8_t *r, uint8_t *g, uint8_t *b) {
    if (val == 0) {
        *r = 0; *g = 0; *b = 0;
        return;
    }
    uint8_t region = hue / 43;
    uint8_t remainder = (hue - (region * 43)) * 6;
    uint8_t q = (val * (255 - remainder)) >> 8;
    uint8_t t = (val * remainder) >> 8;
    switch (region) {
        case 0: *r = val; *g = t; *b = 0; break;
        case 1: *r = q; *g = val; *b = 0; break;
        case 2: *r = 0; *g = val; *b = t; break;
        case 3: *r = 0; *g = q; *b = val; break;
        case 4: *r = t; *g = 0; *b = val; break;
        default: *r = val; *g = 0; *b = q; break;
    }
}

bool led_engine_render_frame(const led_engine_cmd_t *cmd, uint32_t frame_index, rgb_color_t *out_pixels, uint16_t num_leds) {
    if (cmd == NULL || out_pixels == NULL || num_leds == 0) {
        return false;
    }

    uint8_t bright = (cmd->brightness > 100) ? 100 : cmd->brightness;
    uint8_t spd = (cmd->speed == 0) ? 1 : cmd->speed;

    switch (cmd->pattern_id) {
        case PATTERN_OFF: {
            for (uint16_t i = 0; i < num_leds; i++) {
                out_pixels[i].r = 0;
                out_pixels[i].g = 0;
                out_pixels[i].b = 0;
            }
            break;
        }

        case PATTERN_STATIC: {
            uint8_t sr = (uint16_t)cmd->red * bright / 100;
            uint8_t sg = (uint16_t)cmd->green * bright / 100;
            uint8_t sb = (uint16_t)cmd->blue * bright / 100;
            for (uint16_t i = 0; i < num_leds; i++) {
                out_pixels[i].r = sr;
                out_pixels[i].g = sg;
                out_pixels[i].b = sb;
            }
            break;
        }

        case PATTERN_RAINBOW_WAVE: {
            uint8_t val = (255 * bright) / 100;
            for (uint16_t i = 0; i < num_leds; i++) {
                uint8_t hue = (uint8_t)(((i * 256 / num_leds) + (frame_index * spd / 5)) % 256);
                hsv_to_rgb(hue, val, &out_pixels[i].r, &out_pixels[i].g, &out_pixels[i].b);
            }
            break;
        }

        case PATTERN_SPARKLE: {
            uint8_t dim_r = (uint16_t)cmd->red * bright / 300;
            uint8_t dim_g = (uint16_t)cmd->green * bright / 300;
            uint8_t dim_b = (uint16_t)cmd->blue * bright / 300;

            uint8_t bright_r = (uint16_t)cmd->red * bright / 100;
            uint8_t bright_g = (uint16_t)cmd->green * bright / 100;
            uint8_t bright_b = (uint16_t)cmd->blue * bright / 100;

            for (uint16_t i = 0; i < num_leds; i++) {
                uint32_t hash = (i * 2654435761u + frame_index * spd * 1103515245u) & 0xFFFFFF;
                if ((hash % 100) < 15) {
                    out_pixels[i].r = bright_r;
                    out_pixels[i].g = bright_g;
                    out_pixels[i].b = bright_b;
                } else {
                    out_pixels[i].r = dim_r;
                    out_pixels[i].g = dim_g;
                    out_pixels[i].b = dim_b;
                }
            }
            break;
        }

        case PATTERN_FESTIVE_CANDLE: {
            uint8_t base_r = (uint16_t)cmd->red * bright / 100;
            uint8_t base_g = (uint16_t)cmd->green * bright / 100;
            uint8_t base_b = (uint16_t)cmd->blue * bright / 100;

            for (uint16_t i = 0; i < num_leds; i++) {
                uint32_t flicker = ((i * 17 + frame_index * spd * 7) % 50);
                out_pixels[i].r = base_r;
                out_pixels[i].g = (base_g * (50 + flicker)) / 100;
                out_pixels[i].b = base_b / 4;
            }
            break;
        }

        case PATTERN_TWINKLE: {
            for (uint16_t i = 0; i < num_leds; i++) {
                uint8_t phase = (uint8_t)((i * 37 + frame_index * spd / 2) % 256);
                uint8_t factor = (phase < 128) ? (phase * 2) : ((255 - phase) * 2);

                out_pixels[i].r = (uint16_t)cmd->red * bright / 100 * factor / 255;
                out_pixels[i].g = (uint16_t)cmd->green * bright / 100 * factor / 255;
                out_pixels[i].b = (uint16_t)cmd->blue * bright / 100 * factor / 255;
            }
            break;
        }

        default:
            return false;
    }

    return true;
}
