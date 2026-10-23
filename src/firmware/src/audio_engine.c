#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <ctype.h>
#include "../include/audio_engine.h"

void *xAudioQueue = NULL;
static audio_engine_cmd_t s_current_audio_state = {
    .melody_id = MELODY_SILENT,
    .playback_state = AUDIO_STATE_STOP,
    .volume = 50,
    .tempo = 100
};

static const char *s_rtttl_jingle_bells = "JingleBells:d=4,o=5,b=125:8g,8e,8d,c,2g,8g,8e,8d,c,2a,8a,8f,8e,d,2b,8g,8g,8f,8d,e";
static const char *s_rtttl_we_wish_you = "WeWishYou:d=4,o=5,b=140:d,g,8g,8a,8g,8f#,e,c,e,a,8a,8b,8a,8g,f#,d,f#,b,8b,8c6,8b,8a,g,e,8d,8d,e,a,f#,2g";
static const char *s_rtttl_silent_night = "SilentNight:d=4,o=5,b=90:4.g,8a,g,2.e,4.g,8a,g,2.e,2d6,2d6,2.b,2c6,2c6,2.g";
static const char *s_rtttl_deck_the_halls = "DeckTheHalls:d=4,o=5,b=140:4.g,8f,e,d,c,d,e,c,8d,8e,f,d,4.e,8d,c,b4,2c";

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

const char *audio_engine_get_rtttl_score(melody_id_t melody_id) {
    switch (melody_id) {
        case MELODY_SILENT:
            return "";
        case MELODY_JINGLE_BELLS:
            return s_rtttl_jingle_bells;
        case MELODY_WE_WISH_YOU:
            return s_rtttl_we_wish_you;
        case MELODY_SILENT_NIGHT:
            return s_rtttl_silent_night;
        case MELODY_DECK_THE_HALLS:
            return s_rtttl_deck_the_halls;
        default:
            return NULL;
    }
}

uint16_t audio_engine_note_to_freq(char note, bool is_sharp, uint8_t octave) {
    char lower_note = (char)tolower((unsigned char)note);
    if (lower_note == 'p' || lower_note == 'r') {
        return 0; // Pause / rest
    }

    uint16_t base_freq = 0;
    switch (lower_note) {
        case 'c': base_freq = is_sharp ? 277 : 262; break;
        case 'd': base_freq = is_sharp ? 311 : 294; break;
        case 'e': base_freq = 330; break;
        case 'f': base_freq = is_sharp ? 370 : 349; break;
        case 'g': base_freq = is_sharp ? 415 : 392; break;
        case 'a': base_freq = is_sharp ? 466 : 440; break;
        case 'b': base_freq = 494; break;
        default: return 0;
    }

    if (octave < 1 || octave > 8) {
        return 0;
    }

    if (octave == 4) {
        return base_freq;
    } else if (octave > 4) {
        return base_freq << (octave - 4);
    } else {
        return base_freq >> (4 - octave);
    }
}

bool audio_engine_parse_rtttl_defaults(const char *rtttl, uint8_t *default_duration, uint8_t *default_octave, uint16_t *bpm) {
    if (rtttl == NULL || default_duration == NULL || default_octave == NULL || bpm == NULL) {
        return false;
    }

    *default_duration = 4;
    *default_octave = 6;
    *bpm = 63;

    const char *first_colon = strchr(rtttl, ':');
    if (first_colon == NULL) {
        return false;
    }

    const char *second_colon = strchr(first_colon + 1, ':');
    if (second_colon == NULL) {
        return false;
    }

    size_t section_len = second_colon - (first_colon + 1);
    char buf[64];
    if (section_len >= sizeof(buf)) {
        return false;
    }

    memcpy(buf, first_colon + 1, section_len);
    buf[section_len] = '\0';

    char *token = strtok(buf, ",");
    while (token != NULL) {
        while (*token == ' ') token++;
        if (strncmp(token, "d=", 2) == 0) {
            *default_duration = (uint8_t)atoi(token + 2);
        } else if (strncmp(token, "o=", 2) == 0) {
            *default_octave = (uint8_t)atoi(token + 2);
        } else if (strncmp(token, "b=", 2) == 0) {
            *bpm = (uint16_t)atoi(token + 2);
        }
        token = strtok(NULL, ",");
    }

    return true;
}
