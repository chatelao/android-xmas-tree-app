#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <ctype.h>
#include "../include/audio_engine.h"

#ifdef ESP_PLATFORM
#include "driver/ledc.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#endif

void *xAudioQueue = NULL;
static audio_engine_cmd_t s_current_audio_state = {
    .melody_id = MELODY_SILENT,
    .playback_state = AUDIO_STATE_STOP,
    .volume = 50,
    .tempo = 100
};

static audio_ledc_config_t s_ledc_config = {
    .gpio_pin = SPEAKER_LEDC_GPIO_PIN,
    .timer_num = SPEAKER_LEDC_TIMER,
    .channel_num = SPEAKER_LEDC_CHANNEL,
    .duty_resolution_bits = SPEAKER_LEDC_RES_BITS,
    .default_freq_hz = SPEAKER_LEDC_DEFAULT_FREQ_HZ,
    .current_freq_hz = 0,
    .current_duty = 0,
    .is_initialized = false
};

static audio_playback_state_t s_playback_state = {
    .is_playing = false,
    .is_paused = false,
    .active_melody = MELODY_SILENT,
    .cursor = NULL,
    .default_duration = 4,
    .default_octave = 5,
    .bpm = 120,
    .current_freq_hz = 0,
    .current_note_duration_ms = 0
};

static const char *s_rtttl_jingle_bells = "JingleBells:d=4,o=5,b=125:8g,8e,8d,c,2g,8g,8e,8d,c,2a,8a,8f,8e,d,2b,8g,8g,8f,8d,e";
static const char *s_rtttl_we_wish_you = "WeWishYou:d=4,o=5,b=140:d,g,8g,8a,8g,8f#,e,c,e,a,8a,8b,8a,8g,f#,d,f#,b,8b,8c6,8b,8a,g,e,8d,8d,e,a,f#,2g";
static const char *s_rtttl_silent_night = "SilentNight:d=4,o=5,b=90:4.g,8a,g,2.e,4.g,8a,g,2.e,2d6,2d6,2.b,2c6,2c6,2.g";
static const char *s_rtttl_deck_the_halls = "DeckTheHalls:d=4,o=5,b=140:4.g,8f,e,d,c,d,e,c,8d,8e,f,d,4.e,8d,c,b4,2c";

static const char *find_rtttl_notes_start(const char *rtttl) {
    if (rtttl == NULL) {
        return NULL;
    }
    const char *first_colon = strchr(rtttl, ':');
    if (first_colon == NULL) {
        return NULL;
    }
    const char *second_colon = strchr(first_colon + 1, ':');
    if (second_colon == NULL) {
        return NULL;
    }
    return second_colon + 1;
}

void audio_engine_init(void) {
    s_current_audio_state.melody_id = MELODY_SILENT;
    s_current_audio_state.playback_state = AUDIO_STATE_STOP;
    s_current_audio_state.volume = 50;
    s_current_audio_state.tempo = 100;

    memset(&s_playback_state, 0, sizeof(s_playback_state));
    s_playback_state.active_melody = MELODY_SILENT;
    s_playback_state.default_duration = 4;
    s_playback_state.default_octave = 5;
    s_playback_state.bpm = 120;

    audio_engine_ledc_init();
}

audio_ledc_config_t audio_engine_get_ledc_config(void) {
    return s_ledc_config;
}

uint32_t audio_engine_calc_duty(uint8_t volume, uint8_t resolution_bits) {
    if (resolution_bits == 0 || resolution_bits > 16) {
        return 0;
    }
    uint8_t clamped_vol = (volume > 100) ? 100 : volume;
    // 50% duty cycle yields maximum acoustic output for a passive buzzer/speaker
    uint32_t max_half_duty = 1UL << (resolution_bits - 1);
    return (max_half_duty * (uint32_t)clamped_vol) / 100UL;
}

bool audio_engine_ledc_init(void) {
    s_ledc_config.gpio_pin = SPEAKER_LEDC_GPIO_PIN;
    s_ledc_config.timer_num = SPEAKER_LEDC_TIMER;
    s_ledc_config.channel_num = SPEAKER_LEDC_CHANNEL;
    s_ledc_config.duty_resolution_bits = SPEAKER_LEDC_RES_BITS;
    s_ledc_config.default_freq_hz = SPEAKER_LEDC_DEFAULT_FREQ_HZ;
    s_ledc_config.current_freq_hz = 0;
    s_ledc_config.current_duty = 0;
    s_ledc_config.is_initialized = true;

#ifdef ESP_PLATFORM
    ledc_timer_config_t timer_conf = {
        .speed_mode = LEDC_LOW_SPEED_MODE,
        .duty_resolution = (ledc_timer_bit_t)SPEAKER_LEDC_RES_BITS,
        .timer_num = (ledc_timer_t)SPEAKER_LEDC_TIMER,
        .freq_hz = SPEAKER_LEDC_DEFAULT_FREQ_HZ,
        .clk_cfg = LEDC_AUTO_CLK
    };
    if (ledc_timer_config(&timer_conf) != ESP_OK) {
        s_ledc_config.is_initialized = false;
        return false;
    }

    ledc_channel_config_t channel_conf = {
        .gpio_num = SPEAKER_LEDC_GPIO_PIN,
        .speed_mode = LEDC_LOW_SPEED_MODE,
        .channel = (ledc_channel_t)SPEAKER_LEDC_CHANNEL,
        .intr_type = LEDC_INTR_DISABLE,
        .timer_sel = (ledc_timer_t)SPEAKER_LEDC_TIMER,
        .duty = 0,
        .hpoint = 0
    };
    if (ledc_channel_config(&channel_conf) != ESP_OK) {
        s_ledc_config.is_initialized = false;
        return false;
    }
#endif

    return true;
}

bool audio_engine_ledc_set_tone(uint16_t freq_hz, uint8_t volume) {
    if (!s_ledc_config.is_initialized) {
        if (!audio_engine_ledc_init()) {
            return false;
        }
    }

    if (freq_hz == 0 || volume == 0) {
        audio_engine_ledc_stop();
        return true;
    }

    uint32_t duty = audio_engine_calc_duty(volume, s_ledc_config.duty_resolution_bits);
    s_ledc_config.current_freq_hz = freq_hz;
    s_ledc_config.current_duty = duty;

#ifdef ESP_PLATFORM
    if (ledc_set_freq(LEDC_LOW_SPEED_MODE, (ledc_timer_t)SPEAKER_LEDC_TIMER, freq_hz) != ESP_OK) {
        return false;
    }
    if (ledc_set_duty(LEDC_LOW_SPEED_MODE, (ledc_channel_t)SPEAKER_LEDC_CHANNEL, duty) != ESP_OK) {
        return false;
    }
    if (ledc_update_duty(LEDC_LOW_SPEED_MODE, (ledc_channel_t)SPEAKER_LEDC_CHANNEL) != ESP_OK) {
        return false;
    }
#endif

    return true;
}

void audio_engine_ledc_stop(void) {
    s_ledc_config.current_freq_hz = 0;
    s_ledc_config.current_duty = 0;

#ifdef ESP_PLATFORM
    if (s_ledc_config.is_initialized) {
        ledc_set_duty(LEDC_LOW_SPEED_MODE, (ledc_channel_t)SPEAKER_LEDC_CHANNEL, 0);
        ledc_update_duty(LEDC_LOW_SPEED_MODE, (ledc_channel_t)SPEAKER_LEDC_CHANNEL);
    }
#endif
}

void audio_engine_task(void *param) {
    (void)param;
    audio_engine_ledc_init();

#ifdef ESP_PLATFORM
    for (;;) {
        if (audio_engine_playback_step()) {
            uint32_t delay_ms = s_playback_state.current_note_duration_ms;
            if (delay_ms < 20) {
                delay_ms = 20;
            }
            vTaskDelay(pdMS_TO_TICKS(delay_ms));
        } else {
            vTaskDelay(pdMS_TO_TICKS(20));
        }
    }
#else
    audio_engine_playback_step();
#endif
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

bool audio_engine_parse_next_note(const char **cursor, uint8_t default_duration, uint8_t default_octave, uint16_t bpm, uint8_t tempo_pct, audio_note_event_t *out_event) {
    if (cursor == NULL || *cursor == NULL || out_event == NULL || bpm == 0) {
        return false;
    }

    const char *p = *cursor;
    while (*p == ' ' || *p == ',') {
        p++;
    }
    if (*p == '\0') {
        *cursor = p;
        return false;
    }

    uint16_t duration = (default_duration > 0) ? default_duration : 4;
    if (isdigit((unsigned char)*p)) {
        uint16_t parsed_dur = 0;
        while (isdigit((unsigned char)*p)) {
            parsed_dur = (parsed_dur * 10) + (uint16_t)(*p - '0');
            p++;
        }
        if (parsed_dur > 0) {
            duration = parsed_dur;
        }
    }

    bool is_dotted = false;
    if (*p == '.') {
        is_dotted = true;
        p++;
    }

    if (*p == '\0') {
        return false;
    }

    char note_char = *p++;
    bool is_sharp = false;
    if (*p == '#') {
        is_sharp = true;
        p++;
    }

    if (*p == '.') {
        is_dotted = true;
        p++;
    }

    uint8_t octave = default_octave;
    if (isdigit((unsigned char)*p)) {
        octave = (uint8_t)(*p - '0');
        p++;
    }

    if (*p == '.') {
        is_dotted = true;
        p++;
    }

    while (*p == ' ') {
        p++;
    }
    if (*p == ',') {
        p++;
    }
    *cursor = p;

    uint32_t whole_note_ms = (60000UL * 4UL) / (uint32_t)bpm;
    uint32_t note_ms = whole_note_ms / (uint32_t)duration;
    if (is_dotted) {
        note_ms += note_ms / 2UL;
    }

    uint8_t clamped_tempo = tempo_pct;
    if (clamped_tempo < 50) {
        clamped_tempo = 50;
    } else if (clamped_tempo > 200) {
        clamped_tempo = 200;
    }
    note_ms = (note_ms * 100UL) / (uint32_t)clamped_tempo;

    out_event->freq_hz = audio_engine_note_to_freq(note_char, is_sharp, octave);
    out_event->duration_ms = note_ms;
    return true;
}

bool audio_engine_playback_step(void) {
    if (s_current_audio_state.playback_state == AUDIO_STATE_STOP ||
        s_current_audio_state.melody_id == MELODY_SILENT) {
        audio_engine_ledc_stop();
        s_playback_state.is_playing = false;
        s_playback_state.is_paused = false;
        s_playback_state.active_melody = MELODY_SILENT;
        s_playback_state.cursor = NULL;
        s_playback_state.current_freq_hz = 0;
        s_playback_state.current_note_duration_ms = 0;
        return false;
    }

    if (s_current_audio_state.playback_state == AUDIO_STATE_PAUSE) {
        audio_engine_ledc_set_tone(0, 0);
        s_playback_state.is_playing = false;
        s_playback_state.is_paused = true;
        s_playback_state.current_freq_hz = 0;
        return true;
    }

    // AUDIO_STATE_PLAY
    if ((!s_playback_state.is_playing && !s_playback_state.is_paused) ||
        s_playback_state.active_melody != s_current_audio_state.melody_id ||
        s_playback_state.cursor == NULL) {
        const char *score = audio_engine_get_rtttl_score(s_current_audio_state.melody_id);
        const char *notes_start = find_rtttl_notes_start(score);
        if (score == NULL || notes_start == NULL) {
            audio_engine_ledc_stop();
            s_playback_state.is_playing = false;
            return false;
        }
        audio_engine_parse_rtttl_defaults(
            score,
            &s_playback_state.default_duration,
            &s_playback_state.default_octave,
            &s_playback_state.bpm
        );
        s_playback_state.active_melody = s_current_audio_state.melody_id;
        s_playback_state.cursor = notes_start;
    }

    s_playback_state.is_playing = true;
    s_playback_state.is_paused = false;

    audio_note_event_t event = {0};
    if (!audio_engine_parse_next_note(
            &s_playback_state.cursor,
            s_playback_state.default_duration,
            s_playback_state.default_octave,
            s_playback_state.bpm,
            s_current_audio_state.tempo,
            &event)) {
        // Loop back to start of melody
        const char *score = audio_engine_get_rtttl_score(s_playback_state.active_melody);
        const char *notes_start = find_rtttl_notes_start(score);
        if (notes_start == NULL ||
            !audio_engine_parse_next_note(
                &notes_start,
                s_playback_state.default_duration,
                s_playback_state.default_octave,
                s_playback_state.bpm,
                s_current_audio_state.tempo,
                &event)) {
            audio_engine_ledc_stop();
            s_playback_state.is_playing = false;
            return false;
        }
        s_playback_state.cursor = notes_start;
    }

    audio_engine_ledc_set_tone(event.freq_hz, s_current_audio_state.volume);
    s_playback_state.current_freq_hz = event.freq_hz;
    s_playback_state.current_note_duration_ms = event.duration_ms;
    return true;
}

void audio_engine_get_playback_state(audio_playback_state_t *out_playback) {
    if (out_playback != NULL) {
        *out_playback = s_playback_state;
    }
}
