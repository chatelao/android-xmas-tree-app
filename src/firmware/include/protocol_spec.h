#ifndef PROTOCOL_SPEC_H
#define PROTOCOL_SPEC_H

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

#define LIGHTING_PAYLOAD_SIZE 8
#define AUDIO_PAYLOAD_SIZE 6
#define TELEMETRY_PAYLOAD_SIZE 10

// Opcodes for Lighting Control
typedef enum {
    CMD_LIGHTING_SET_PATTERN      = 0x01,
    CMD_LIGHTING_SET_STATIC_COLOR = 0x02,
    CMD_LIGHTING_SET_BRIGHTNESS   = 0x03
} lighting_cmd_id_t;

// Pattern IDs
typedef enum {
    PATTERN_OFF            = 0,
    PATTERN_STATIC         = 1,
    PATTERN_RAINBOW_WAVE   = 2,
    PATTERN_SPARKLE        = 3,
    PATTERN_FESTIVE_CANDLE = 4,
    PATTERN_TWINKLE        = 5
} pattern_id_t;

// Opcodes for Audio Control
typedef enum {
    CMD_AUDIO_PLAY_SONG       = 0x10,
    CMD_AUDIO_STOP            = 0x11,
    CMD_AUDIO_PLAY_FREQ_TONE  = 0x12,
    CMD_AUDIO_SET_VOLUME      = 0x13
} audio_cmd_id_t;

// Melody IDs
typedef enum {
    MELODY_SILENT        = 0,
    MELODY_JINGLE_BELLS  = 1,
    MELODY_WE_WISH_YOU   = 2,
    MELODY_SILENT_NIGHT  = 3,
    MELODY_DECK_THE_HALLS= 4
} melody_id_t;

// Playback States
typedef enum {
    AUDIO_STATE_STOP  = 0x00,
    AUDIO_STATE_PLAY  = 0x01,
    AUDIO_STATE_PAUSE = 0x02
} audio_state_t;

// Error Codes
typedef enum {
    ERROR_CODE_OK               = 0x00,
    ERROR_CODE_NVS_ERROR        = 0x01,
    ERROR_CODE_LED_TIMING_ERROR = 0x02
} system_error_code_t;

// Packing pragma to ensure 1-byte alignment matching exact byte offsets
#pragma pack(push, 1)

typedef struct {
    uint8_t command_id;
    uint8_t pattern_id;
    uint8_t red;
    uint8_t green;
    uint8_t blue;
    uint8_t brightness;
    uint8_t speed;
    uint8_t reserved;
} lighting_control_payload_t;

typedef struct {
    uint8_t command_id;
    uint8_t melody_id;
    uint8_t playback_state;
    uint8_t volume;
    uint8_t tempo;
    uint8_t reserved;
} audio_control_payload_t;

typedef struct {
    uint32_t system_uptime;
    uint8_t current_pattern;
    uint8_t current_melody;
    uint8_t current_brightness;
    uint8_t current_volume;
    uint8_t connection_state;
    uint8_t error_code;
} telemetry_payload_t;

#pragma pack(pop)

// Parse / Serialize Function Return Status
typedef enum {
    PROTOCOL_OK                   = 0,
    PROTOCOL_ERROR_INVALID_SIZE   = -1,
    PROTOCOL_ERROR_NULL_POINTER   = -2,
    PROTOCOL_ERROR_INVALID_CMD    = -3,
    PROTOCOL_ERROR_INVALID_PARAM  = -4
} protocol_status_t;

// Function Prototypes
protocol_status_t parse_lighting_control_frame(const uint8_t *buffer, size_t size, lighting_control_payload_t *out_payload);
protocol_status_t parse_audio_control_frame(const uint8_t *buffer, size_t size, audio_control_payload_t *out_payload);

protocol_status_t serialize_telemetry_frame(const telemetry_payload_t *payload, uint8_t *out_buffer, size_t buffer_size);
protocol_status_t parse_telemetry_frame(const uint8_t *buffer, size_t size, telemetry_payload_t *out_payload);

#ifdef __cplusplus
}
#endif

#endif // PROTOCOL_SPEC_H
