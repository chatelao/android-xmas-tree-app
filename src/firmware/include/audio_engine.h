#ifndef AUDIO_ENGINE_H
#define AUDIO_ENGINE_H

#include <stdbool.h>
#include <stdint.h>
#include "protocol_spec.h"

#define SPEAKER_LEDC_GPIO_PIN 19

typedef struct {
    melody_id_t melody_id;
    audio_state_t playback_state;
    uint8_t volume;
    uint8_t tempo;
} audio_engine_cmd_t;

typedef struct {
    uint8_t gpio_pin;
    uint8_t timer_num;
    uint8_t channel;
    uint8_t duty_resolution_bits;
    uint32_t base_freq_hz;
} ledc_speaker_config_t;

typedef struct {
    uint16_t frequency_hz;
    uint32_t duration_ms;
} audio_note_t;

// Queue handle declaration stub
extern void *xAudioQueue;

void audio_engine_task(void *param);
void audio_engine_init(void);
bool audio_engine_send_command(const audio_engine_cmd_t *cmd);
void audio_engine_get_current_state(audio_engine_cmd_t *out_state);

// LEDC PWM Driver Configuration & Tone Generation Helper Functions
ledc_speaker_config_t audio_engine_get_ledc_config(void);
uint32_t audio_engine_calculate_duty(uint8_t volume, uint8_t duty_bits);

// Tone and Melody Playback Controller Interface
bool audio_engine_start_melody(melody_id_t melody_id, uint8_t volume, uint8_t tempo);
void audio_engine_stop_melody(void);
bool audio_engine_play_tone(uint16_t freq_hz, uint32_t duration_ms, uint8_t volume);
bool audio_engine_step_playback(audio_note_t *out_note);
bool audio_engine_get_current_note(audio_note_t *out_note);

// RTTTL Melody Score & Frequency Helper Functions
const char *audio_engine_get_rtttl_score(melody_id_t melody_id);
uint16_t audio_engine_note_to_freq(char note, bool is_sharp, uint8_t octave);
bool audio_engine_parse_rtttl_defaults(const char *rtttl, uint8_t *default_duration, uint8_t *default_octave, uint16_t *bpm);
bool audio_engine_parse_next_note(const char **melody_ptr, uint8_t def_dur, uint8_t def_oct, uint16_t bpm, uint8_t tempo_modifier, audio_note_t *out_note);

#endif // AUDIO_ENGINE_H
