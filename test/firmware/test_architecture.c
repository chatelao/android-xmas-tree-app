#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "../../src/firmware/include/ble_manager.h"
#include "../../src/firmware/include/led_engine.h"
#include "../../src/firmware/include/audio_engine.h"
#include "../../src/firmware/include/nvs_manager.h"

static bool s_lighting_cb_called = false;
static bool s_audio_cb_called = false;

static void on_lighting_cmd(const lighting_control_payload_t *payload) {
    if (payload != NULL) {
        s_lighting_cb_called = true;
    }
}

static void on_audio_cmd(const audio_control_payload_t *payload) {
    if (payload != NULL) {
        s_audio_cb_called = true;
    }
}

void test_ble_manager_stub(void) {
    printf("Testing BLE Manager stub...\n");
    ble_manager_init();
    assert(ble_manager_get_state() == BLE_STATE_DISCONNECTED);

    nimble_host_task(NULL);
    assert(ble_manager_get_state() == BLE_STATE_ADVERTISING);

    telemetry_payload_t telemetry = {0};
    assert(ble_manager_send_telemetry(&telemetry) == false); // Not connected
    printf("  PASSED: BLE Manager stub tests\n");
}

void test_rtttl_parser_and_freq(void) {
    printf("Testing RTTTL parser and note frequency converter...\n");

    const char *jingle = audio_engine_get_rtttl_score(MELODY_JINGLE_BELLS);
    assert(jingle != NULL && strstr(jingle, "JingleBells") != NULL);

    const char *we_wish = audio_engine_get_rtttl_score(MELODY_WE_WISH_YOU);
    assert(we_wish != NULL && strstr(we_wish, "WeWishYou") != NULL);

    uint8_t duration = 0, octave = 0;
    uint16_t bpm = 0;
    bool parsed = audio_engine_parse_rtttl_defaults(jingle, &duration, &octave, &bpm);
    assert(parsed == true);
    assert(duration == 4);
    assert(octave == 5);
    assert(bpm == 125);

    // Test note-to-frequency conversion
    assert(audio_engine_note_to_freq('c', false, 4) == 262);
    assert(audio_engine_note_to_freq('a', false, 4) == 440);
    assert(audio_engine_note_to_freq('c', true, 5) == 554); // 277 * 2
    assert(audio_engine_note_to_freq('p', false, 4) == 0);  // Rest

    printf("  PASSED: RTTTL parser and note frequency converter tests\n");
}

void test_ble_cmd_dispatcher(void) {
    printf("Testing BLE Command Dispatcher...\n");
    ble_manager_init();
    led_engine_init();
    audio_engine_init();

    ble_manager_register_callbacks(on_lighting_cmd, on_audio_cmd);

    // Valid 8-byte lighting control frame (SET_PATTERN, SPARKLE, R=200, G=100, B=50, Brightness=90, Speed=60, 0)
    uint8_t lighting_frame[8] = { 0x01, PATTERN_SPARKLE, 200, 100, 50, 90, 60, 0x00 };
    assert(ble_manager_process_lighting_cmd(lighting_frame, sizeof(lighting_frame)) == true);
    assert(s_lighting_cb_called == true);

    led_engine_cmd_t led_state = {0};
    led_engine_get_current_state(&led_state);
    assert(led_state.pattern_id == PATTERN_SPARKLE);
    assert(led_state.red == 200);
    assert(led_state.brightness == 90);

    // Valid 6-byte audio control frame (PLAY_SONG, DECK_THE_HALLS, PLAY, Volume=75, Tempo=110, 0)
    uint8_t audio_frame[6] = { 0x10, MELODY_DECK_THE_HALLS, AUDIO_STATE_PLAY, 75, 110, 0x00 };
    assert(ble_manager_process_audio_cmd(audio_frame, sizeof(audio_frame)) == true);
    assert(s_audio_cb_called == true);

    audio_engine_cmd_t audio_state = {0};
    audio_engine_get_current_state(&audio_state);
    assert(audio_state.melody_id == MELODY_DECK_THE_HALLS);
    assert(audio_state.volume == 75);

    // Invalid frame sizes
    assert(ble_manager_process_lighting_cmd(lighting_frame, 5) == false);
    assert(ble_manager_process_audio_cmd(audio_frame, 4) == false);

    printf("  PASSED: BLE Command Dispatcher tests\n");
}

void test_led_engine_stub(void) {
    printf("Testing LED Engine stub...\n");
    led_engine_init();
    led_engine_cmd_t cmd = {
        .pattern_id = PATTERN_RAINBOW_WAVE,
        .red = 255,
        .green = 128,
        .blue = 0,
        .brightness = 75,
        .speed = 80
    };

    assert(led_engine_send_command(&cmd) == true);

    led_engine_cmd_t state = {0};
    led_engine_get_current_state(&state);
    assert(state.pattern_id == PATTERN_RAINBOW_WAVE);
    assert(state.red == 255);
    assert(state.green == 128);
    assert(state.blue == 0);
    assert(state.brightness == 75);
    assert(state.speed == 80);

    printf("  PASSED: LED Engine stub tests\n");
}

void test_led_engine_rendering_and_rmt(void) {
    printf("Testing LED Engine WS2812B RMT timing and rendering algorithms...\n");

    // Test RMT timing config
    ws2812b_rmt_config_t timing = led_engine_get_rmt_timing();
    assert(timing.gpio_pin == LED_STRIP_GPIO_PIN);
    assert(timing.t0h_ns == 350);
    assert(timing.t0l_ns == 800);
    assert(timing.t1h_ns == 700);
    assert(timing.t1l_ns == 600);

    rgb_color_t pixels[LED_STRIP_NUM_LEDS] = {0};

    // Test PATTERN_OFF
    led_engine_cmd_t off_cmd = {
        .pattern_id = PATTERN_OFF,
        .red = 255, .green = 255, .blue = 255,
        .brightness = 100, .speed = 50
    };
    assert(led_engine_render_frame(&off_cmd, 0, pixels, LED_STRIP_NUM_LEDS) == true);
    for (int i = 0; i < LED_STRIP_NUM_LEDS; i++) {
        assert(pixels[i].r == 0 && pixels[i].g == 0 && pixels[i].b == 0);
    }

    // Test PATTERN_STATIC
    led_engine_cmd_t static_cmd = {
        .pattern_id = PATTERN_STATIC,
        .red = 200, .green = 100, .blue = 50,
        .brightness = 50, .speed = 50
    };
    assert(led_engine_render_frame(&static_cmd, 0, pixels, LED_STRIP_NUM_LEDS) == true);
    for (int i = 0; i < LED_STRIP_NUM_LEDS; i++) {
        assert(pixels[i].r == 100);
        assert(pixels[i].g == 50);
        assert(pixels[i].b == 25);
    }

    // Test PATTERN_RAINBOW_WAVE
    led_engine_cmd_t rainbow_cmd = {
        .pattern_id = PATTERN_RAINBOW_WAVE,
        .red = 0, .green = 0, .blue = 0,
        .brightness = 100, .speed = 50
    };
    assert(led_engine_render_frame(&rainbow_cmd, 0, pixels, LED_STRIP_NUM_LEDS) == true);
    // Ensure rendered pixels are not all zero
    bool non_zero = false;
    for (int i = 0; i < LED_STRIP_NUM_LEDS; i++) {
        if (pixels[i].r > 0 || pixels[i].g > 0 || pixels[i].b > 0) non_zero = true;
    }
    assert(non_zero == true);

    // Test PATTERN_SPARKLE
    led_engine_cmd_t sparkle_cmd = {
        .pattern_id = PATTERN_SPARKLE,
        .red = 255, .green = 0, .blue = 0,
        .brightness = 100, .speed = 50
    };
    assert(led_engine_render_frame(&sparkle_cmd, 1, pixels, LED_STRIP_NUM_LEDS) == true);

    // Test PATTERN_FESTIVE_CANDLE
    led_engine_cmd_t candle_cmd = {
        .pattern_id = PATTERN_FESTIVE_CANDLE,
        .red = 255, .green = 150, .blue = 0,
        .brightness = 80, .speed = 30
    };
    assert(led_engine_render_frame(&candle_cmd, 2, pixels, LED_STRIP_NUM_LEDS) == true);

    // Test PATTERN_TWINKLE
    led_engine_cmd_t twinkle_cmd = {
        .pattern_id = PATTERN_TWINKLE,
        .red = 255, .green = 255, .blue = 255,
        .brightness = 90, .speed = 40
    };
    assert(led_engine_render_frame(&twinkle_cmd, 5, pixels, LED_STRIP_NUM_LEDS) == true);

    // Test error/null handling
    assert(led_engine_render_frame(NULL, 0, pixels, LED_STRIP_NUM_LEDS) == false);
    assert(led_engine_render_frame(&twinkle_cmd, 0, NULL, LED_STRIP_NUM_LEDS) == false);
    assert(led_engine_render_frame(&twinkle_cmd, 0, pixels, 0) == false);

    printf("  PASSED: LED Engine WS2812B RMT timing and rendering algorithms tests\n");
}

void test_audio_engine_stub(void) {
    printf("Testing Audio Engine stub...\n");
    audio_engine_init();
    audio_engine_cmd_t cmd = {
        .melody_id = MELODY_JINGLE_BELLS,
        .playback_state = AUDIO_STATE_PLAY,
        .volume = 85,
        .tempo = 105
    };

    assert(audio_engine_send_command(&cmd) == true);

    audio_engine_cmd_t state = {0};
    audio_engine_get_current_state(&state);
    assert(state.melody_id == MELODY_JINGLE_BELLS);
    assert(state.playback_state == AUDIO_STATE_PLAY);
    assert(state.volume == 85);
    assert(state.tempo == 105);

    printf("  PASSED: Audio Engine stub tests\n");
}

void test_nvs_manager_stub(void) {
    printf("Testing NVS Manager stub...\n");
    nvs_manager_init();

    nvs_saved_state_t save_state = {
        .pattern_id = PATTERN_SPARKLE,
        .red = 100,
        .green = 200,
        .blue = 255,
        .brightness = 95,
        .speed = 60,
        .melody_id = MELODY_SILENT_NIGHT,
        .volume = 40,
        .tempo = 90
    };

    assert(nvs_save_system_state(&save_state) == true);

    nvs_saved_state_t loaded_state = {0};
    assert(nvs_load_system_state(&loaded_state) == true);
    assert(loaded_state.pattern_id == PATTERN_SPARKLE);
    assert(loaded_state.melody_id == MELODY_SILENT_NIGHT);
    assert(loaded_state.brightness == 95);
    assert(loaded_state.volume == 40);

    printf("  PASSED: NVS Manager stub tests\n");
}

void test_audio_ledc_and_playback_controller(void) {
    printf("Testing Audio Engine LEDC PWM driver and playback controller...\n");
    audio_engine_init();

    // Verify LEDC configuration on GPIO 19, Timer 0, Channel 0, 10-bit resolution
    audio_ledc_config_t ledc = audio_engine_get_ledc_config();
    assert(ledc.is_initialized == true);
    assert(ledc.gpio_pin == SPEAKER_LEDC_GPIO_PIN);
    assert(ledc.gpio_pin == 19);
    assert(ledc.timer_num == 0);
    assert(ledc.channel_num == 0);
    assert(ledc.duty_resolution_bits == 10);
    assert(ledc.current_freq_hz == 0);
    assert(ledc.current_duty == 0);

    // Verify duty cycle calculation (10-bit: 50% max duty = 512)
    assert(audio_engine_calc_duty(0, 10) == 0);
    assert(audio_engine_calc_duty(50, 10) == 256);
    assert(audio_engine_calc_duty(100, 10) == 512);
    assert(audio_engine_calc_duty(150, 10) == 512); // Clamped to 100%

    // Verify direct LEDC tone setting & stopping
    assert(audio_engine_ledc_set_tone(440, 50) == true);
    ledc = audio_engine_get_ledc_config();
    assert(ledc.current_freq_hz == 440);
    assert(ledc.current_duty == 256);

    audio_engine_ledc_stop();
    ledc = audio_engine_get_ledc_config();
    assert(ledc.current_freq_hz == 0);
    assert(ledc.current_duty == 0);

    // Verify RTTTL note sequence parser (Jingle Bells: "8g,8e,8d,c,2g,...")
    const char *notes = "8g,8e,4.f#6,p";
    audio_note_event_t ev = {0};
    assert(audio_engine_parse_next_note(&notes, 4, 5, 120, 100, &ev) == true);
    assert(ev.freq_hz == audio_engine_note_to_freq('g', false, 5));
    assert(ev.duration_ms == 250); // 120 bpm -> quarter=500ms, eighth=250ms

    assert(audio_engine_parse_next_note(&notes, 4, 5, 120, 100, &ev) == true);
    assert(ev.freq_hz == audio_engine_note_to_freq('e', false, 5));
    assert(ev.duration_ms == 250);

    assert(audio_engine_parse_next_note(&notes, 4, 5, 120, 100, &ev) == true);
    assert(ev.freq_hz == audio_engine_note_to_freq('f', true, 6));
    assert(ev.duration_ms == 750); // dotted quarter at 120 bpm = 500 + 250 = 750ms

    assert(audio_engine_parse_next_note(&notes, 4, 5, 120, 200, &ev) == true);
    assert(ev.freq_hz == 0); // Rest
    assert(ev.duration_ms == 250); // quarter at 200% tempo = 500 * 100 / 200 = 250ms

    assert(audio_engine_parse_next_note(&notes, 4, 5, 120, 100, &ev) == false); // End of string

    // Verify playback controller Play -> Pause -> Resume -> Stop
    audio_engine_cmd_t play_cmd = {
        .melody_id = MELODY_JINGLE_BELLS,
        .playback_state = AUDIO_STATE_PLAY,
        .volume = 100,
        .tempo = 100
    };
    assert(audio_engine_send_command(&play_cmd) == true);

    // Step 1: First note of Jingle Bells is 8g (octave 5 = 784 Hz)
    assert(audio_engine_playback_step() == true);
    audio_playback_state_t pb = {0};
    audio_engine_get_playback_state(&pb);
    assert(pb.is_playing == true);
    assert(pb.is_paused == false);
    assert(pb.active_melody == MELODY_JINGLE_BELLS);
    assert(pb.current_freq_hz == 784);
    assert(pb.current_note_duration_ms > 0);
    ledc = audio_engine_get_ledc_config();
    assert(ledc.current_freq_hz == 784);
    assert(ledc.current_duty == 512);

    // Step 2: Second note is 8e (octave 5 = 660 Hz)
    assert(audio_engine_playback_step() == true);
    audio_engine_get_playback_state(&pb);
    assert(pb.current_freq_hz == 660);

    // Pause playback
    play_cmd.playback_state = AUDIO_STATE_PAUSE;
    assert(audio_engine_send_command(&play_cmd) == true);
    assert(audio_engine_playback_step() == true);
    audio_engine_get_playback_state(&pb);
    assert(pb.is_playing == false);
    assert(pb.is_paused == true);
    assert(pb.current_freq_hz == 0);
    ledc = audio_engine_get_ledc_config();
    assert(ledc.current_duty == 0);

    // Resume playback -> third note is 8d (octave 5 = 588 Hz)
    play_cmd.playback_state = AUDIO_STATE_PLAY;
    assert(audio_engine_send_command(&play_cmd) == true);
    assert(audio_engine_playback_step() == true);
    audio_engine_get_playback_state(&pb);
    assert(pb.is_playing == true);
    assert(pb.is_paused == false);
    assert(pb.current_freq_hz == 588);

    // Stop playback
    play_cmd.playback_state = AUDIO_STATE_STOP;
    assert(audio_engine_send_command(&play_cmd) == true);
    assert(audio_engine_playback_step() == false);
    audio_engine_get_playback_state(&pb);
    assert(pb.is_playing == false);
    assert(pb.is_paused == false);
    assert(pb.active_melody == MELODY_SILENT);
    ledc = audio_engine_get_ledc_config();
    assert(ledc.current_freq_hz == 0);
    assert(ledc.current_duty == 0);

    printf("  PASSED: Audio Engine LEDC PWM driver and playback controller tests\n");
}

int main(void) {
    printf("==========================================\n");
    printf("Running Firmware Architecture Unit Tests\n");
    printf("==========================================\n");

    test_ble_manager_stub();
    test_rtttl_parser_and_freq();
    test_ble_cmd_dispatcher();
    test_led_engine_stub();
    test_led_engine_rendering_and_rmt();
    test_audio_engine_stub();
    test_nvs_manager_stub();
    test_audio_ledc_and_playback_controller();

    printf("==========================================\n");
    printf("All Firmware Architecture Tests Passed Successfully!\n");
    printf("==========================================\n");
    return 0;
}
