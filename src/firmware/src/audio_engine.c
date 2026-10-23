#include <stdio.h>
#include <string.h>
#include "../include/audio_engine.h"

void *xAudioQueue = NULL;
static audio_engine_cmd_t s_current_audio_state = {
    .melody_id = MELODY_SILENT,
    .playback_state = AUDIO_STATE_STOP,
    .volume = 50,
    .tempo = 100
};

void audio_engine_init(void) {
    s_current_audio_state.melody_id = MELODY_SILENT;
    s_current_audio_state.playback_state = AUDIO_STATE_STOP;
}

void audio_engine_task(void *param) {
    (void)param;
}

bool audio_engine_send_command(const audio_engine_cmd_t *cmd) {
    if (cmd != NULL) {
        s_current_audio_state = *cmd;
        return true;
    }
    return false;
}

void audio_engine_get_current_state(audio_engine_cmd_t *out_state) {
    if (out_state != NULL) {
        *out_state = s_current_audio_state;
    }
}
