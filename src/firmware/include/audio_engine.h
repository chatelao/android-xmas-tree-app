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

#endif // AUDIO_ENGINE_H
