#ifndef AUDIO_ENGINE_H
#define AUDIO_ENGINE_H

#include <stdbool.h>
#include <stdint.h>
#include "protocol_spec.h"

#define SPEAKER_LEDC_GPIO_PIN 19
#define SPEAKER_LEDC_TIMER    0
#define SPEAKER_LEDC_CHANNEL  0
#define SPEAKER_LEDC_RES_BITS 10
#define SPEAKER_LEDC_DEFAULT_FREQ_HZ 440

typedef struct {
    melody_id_t melody_id;
    audio_state_t playback_state;
    uint8_t volume;
    uint8_t tempo;
} audio_engine_cmd_t;

typedef struct {
    uint8_t gpio_pin;
    uint8_t timer_num;
    uint8_t channel_num;
    uint8_t duty_resolution_bits;
    uint32_t default_freq_hz;
    uint32_t current_freq_hz;
    uint32_t current_duty;
    bool is_initialized;
} audio_ledc_config_t;

typedef struct {
    uint16_t freq_hz;
    uint32_t duration_ms;
} audio_note_event_t;

typedef struct {
    bool is_playing;
    bool is_paused;
    melody_id_t active_melody;
    const char *cursor;
    uint8_t default_duration;
    uint8_t default_octave;
    uint16_t bpm;
    uint16_t current_freq_hz;
    uint32_t current_note_duration_ms;
} audio_playback_state_t;

// Queue handle declaration stub
extern void *xAudioQueue;

void audio_engine_task(void *param);
void audio_engine_init(void);
bool audio_engine_send_command(const audio_engine_cmd_t *cmd);
void audio_engine_get_current_state(audio_engine_cmd_t *out_state);

// LEDC PWM Peripheral Driver Helpers (GPIO 19, Timer 0, Channel 0)
audio_ledc_config_t audio_engine_get_ledc_config(void);
uint32_t audio_engine_calc_duty(uint8_t volume, uint8_t resolution_bits);
bool audio_engine_ledc_init(void);
bool audio_engine_ledc_set_tone(uint16_t freq_hz, uint8_t volume);
void audio_engine_ledc_stop(void);

// RTTTL Melody Score, Frequency & Note Sequence Parser
const char *audio_engine_get_rtttl_score(melody_id_t melody_id);
uint16_t audio_engine_note_to_freq(char note, bool is_sharp, uint8_t octave);
bool audio_engine_parse_rtttl_defaults(const char *rtttl, uint8_t *default_duration, uint8_t *default_octave, uint16_t *bpm);
bool audio_engine_parse_next_note(const char **cursor, uint8_t default_duration, uint8_t default_octave, uint16_t bpm, uint8_t tempo_pct, audio_note_event_t *out_event);

// Tone Playback Controller
bool audio_engine_playback_step(void);
void audio_engine_get_playback_state(audio_playback_state_t *out_playback);

#endif // AUDIO_ENGINE_H
