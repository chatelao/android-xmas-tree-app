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

// Queue handle declaration stub
extern void *xAudioQueue;

void audio_engine_task(void *param);
void audio_engine_init(void);
bool audio_engine_send_command(const audio_engine_cmd_t *cmd);
void audio_engine_get_current_state(audio_engine_cmd_t *out_state);

// RTTTL Melody Score & Frequency Helper Functions
const char *audio_engine_get_rtttl_score(melody_id_t melody_id);
uint16_t audio_engine_note_to_freq(char note, bool is_sharp, uint8_t octave);
bool audio_engine_parse_rtttl_defaults(const char *rtttl, uint8_t *default_duration, uint8_t *default_octave, uint16_t *bpm);

#endif // AUDIO_ENGINE_H
