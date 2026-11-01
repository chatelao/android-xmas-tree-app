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

// Internal playback state
static const char *s_current_melody_ptr = NULL;
static uint8_t s_melody_def_duration = 4;
static uint8_t s_melody_def_octave = 5;
static uint16_t s_melody_bpm = 120;
static audio_note_t s_current_active_note = { .frequency_hz = 0, .duration_ms = 0 };

void audio_engine_init(void) {
    s_current_audio_state.melody_id = MELODY_SILENT;
    s_current_audio_state.playback_state = AUDIO_STATE_STOP;
    s_current_audio_state.volume = 50;
    s_current_audio_state.tempo = 100;
    audio_engine_stop_melody();
}

void audio_engine_task(void *param) {
    (void)param;
    if (s_current_audio_state.playback_state == AUDIO_STATE_PLAY) {
        audio_note_t note;
        if (!audio_engine_step_playback(&note)) {
            // Reached end of melody
            s_current_audio_state.playback_state = AUDIO_STATE_STOP;
        }
    }
}

bool audio_engine_send_command(const audio_engine_cmd_t *cmd) {
    if (cmd != NULL) {
        s_current_audio_state = *cmd;
        if (cmd->playback_state == AUDIO_STATE_PLAY) {
            audio_engine_start_melody(cmd->melody_id, cmd->volume, cmd->tempo);
        } else if (cmd->playback_state == AUDIO_STATE_STOP) {
            audio_engine_stop_melody();
        } else if (cmd->playback_state == AUDIO_STATE_PAUSE) {
            // Keep pointer but mute current note
            s_current_active_note.frequency_hz = 0;
            s_current_active_note.duration_ms = 0;
        }
        return true;
    }
    return false;
}

void audio_engine_get_current_state(audio_engine_cmd_t *out_state) {
    if (out_state != NULL) {
        *out_state = s_current_audio_state;
    }
}

ledc_speaker_config_t audio_engine_get_ledc_config(void) {
    ledc_speaker_config_t config = {
        .gpio_pin = SPEAKER_LEDC_GPIO_PIN, // GPIO 19
        .timer_num = 0,                    // LEDC_TIMER_0
        .channel = 0,                      // LEDC_CHANNEL_0
        .duty_resolution_bits = 10,        // 10-bit resolution (0 - 1023)
        .base_freq_hz = 440                // Base A4 tone
    };
    return config;
}

uint32_t audio_engine_calculate_duty(uint8_t volume, uint8_t duty_bits) {
    if (volume == 0 || duty_bits == 0) {
        return 0; // Muted
    }
    if (volume > 100) {
        volume = 100;
    }

    // Max 50% duty cycle for square wave on buzzer/loudspeaker
    // (1 << duty_bits) is total resolution; 50% is (1 << (duty_bits - 1))
    uint32_t max_duty_50_pct = (1UL << (duty_bits - 1));
    return (max_duty_50_pct * volume) / 100;
}

bool audio_engine_start_melody(melody_id_t melody_id, uint8_t volume, uint8_t tempo) {
    const char *score = audio_engine_get_rtttl_score(melody_id);
    if (score == NULL || *score == '\0') {
        audio_engine_stop_melody();
        return false;
    }

    if (!audio_engine_parse_rtttl_defaults(score, &s_melody_def_duration, &s_melody_def_octave, &s_melody_bpm)) {
        audio_engine_stop_melody();
        return false;
    }

    // Locate start of notes section (after 2nd colon)
    const char *c1 = strchr(score, ':');
    if (!c1) return false;
    const char *c2 = strchr(c1 + 1, ':');
    if (!c2) return false;

    s_current_melody_ptr = c2 + 1;
    s_current_audio_state.melody_id = melody_id;
    s_current_audio_state.playback_state = AUDIO_STATE_PLAY;
    s_current_audio_state.volume = volume;
    s_current_audio_state.tempo = (tempo == 0) ? 100 : tempo;

    return true;
}

bool audio_engine_parse_next_note(const char **melody_ptr, uint8_t def_dur, uint8_t def_oct, uint16_t bpm, uint8_t tempo_modifier, audio_note_t *out_note) {
    if (melody_ptr == NULL || *melody_ptr == NULL || out_note == NULL || bpm == 0) {
        return false;
    }

    const char *p = *melody_ptr;
    // Skip any leading commas or whitespace
    while (*p == ',' || *p == ' ') {
        p++;
    }

    if (*p == '\0') {
        *melody_ptr = p;
        return false;
    }

    // 1. Duration (optional digits at front)
    uint8_t note_duration = def_dur;
    if (isdigit((unsigned char)*p)) {
        note_duration = (uint8_t)atoi(p);
        while (isdigit((unsigned char)*p)) {
            p++;
        }
    }
    if (note_duration == 0) {
        note_duration = def_dur;
    }

    // Check for dot before or after note (standard RTTTL usually has it after note/sharp or before)
    bool is_dotted = false;
    if (*p == '.') {
        is_dotted = true;
        p++;
    }

    // 2. Note letter
    char note_char = *p;
    if (note_char == '\0') {
        *melody_ptr = p;
        return false;
    }
    p++;

    // 3. Sharp symbol '#'
    bool is_sharp = false;
    if (*p == '#') {
        is_sharp = true;
        p++;
    }

    // Dot can also appear after note/sharp
    if (*p == '.') {
        is_dotted = true;
        p++;
    }

    // 4. Octave (optional digit)
    uint8_t note_octave = def_oct;
    if (isdigit((unsigned char)*p)) {
        note_octave = (uint8_t)(*p - '0');
        p++;
    }

    // Dot can also appear after octave
    if (*p == '.') {
        is_dotted = true;
        p++;
    }

    // Skip trailing whitespace or comma delimiter
    while (*p == ' ') {
        p++;
    }
    if (*p == ',') {
        p++;
    }

    *melody_ptr = p;

    // Calculate frequency
    uint16_t freq = audio_engine_note_to_freq(note_char, is_sharp, note_octave);

    // Calculate duration in ms:
    // Whole note (duration=1) in 4/4 time = (60000 ms / bpm) * 4 = 240000 / bpm ms
    // note duration ms = (240000 / bpm) / note_duration
    uint32_t dur_ms = (240000UL / bpm) / note_duration;
    if (is_dotted) {
        dur_ms = dur_ms + (dur_ms / 2);
    }

    // Apply tempo modifier (100 = 100% normal speed; 200 = double speed / half duration)
    uint8_t tempo = (tempo_modifier == 0) ? 100 : tempo_modifier;
    dur_ms = (dur_ms * 100UL) / tempo;

    out_note->frequency_hz = freq;
    out_note->duration_ms = dur_ms;

    return true;
}

void audio_engine_stop_melody(void) {
    s_current_melody_ptr = NULL;
    s_current_active_note.frequency_hz = 0;
    s_current_active_note.duration_ms = 0;
    s_current_audio_state.playback_state = AUDIO_STATE_STOP;
}

bool audio_engine_play_tone(uint16_t freq_hz, uint32_t duration_ms, uint8_t volume) {
    if (volume == 0 || freq_hz == 0) {
        s_current_active_note.frequency_hz = 0;
        s_current_active_note.duration_ms = duration_ms;
    } else {
        s_current_active_note.frequency_hz = freq_hz;
        s_current_active_note.duration_ms = duration_ms;
    }
    s_current_audio_state.volume = volume;
    return true;
}

bool audio_engine_step_playback(audio_note_t *out_note) {
    if (s_current_melody_ptr == NULL || *s_current_melody_ptr == '\0') {
        s_current_active_note.frequency_hz = 0;
        s_current_active_note.duration_ms = 0;
        return false;
    }

    audio_note_t note;
    bool success = audio_engine_parse_next_note(
        &s_current_melody_ptr,
        s_melody_def_duration,
        s_melody_def_octave,
        s_melody_bpm,
        s_current_audio_state.tempo,
        &note
    );

    if (success) {
        s_current_active_note = note;
        if (out_note != NULL) {
            *out_note = note;
        }
        return true;
    }

    s_current_active_note.frequency_hz = 0;
    s_current_active_note.duration_ms = 0;
    return false;
}

bool audio_engine_get_current_note(audio_note_t *out_note) {
    if (out_note != NULL) {
        *out_note = s_current_active_note;
        return true;
    }
    return false;
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
