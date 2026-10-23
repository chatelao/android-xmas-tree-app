#include "protocol_spec.h"
#include <string.h>

protocol_status_t parse_lighting_control_frame(const uint8_t *buffer, size_t size, lighting_control_payload_t *out_payload) {
    if (buffer == NULL || out_payload == NULL) {
        return PROTOCOL_ERROR_NULL_POINTER;
    }
    if (size != LIGHTING_PAYLOAD_SIZE) {
        return PROTOCOL_ERROR_INVALID_SIZE;
    }

    lighting_control_payload_t temp;
    memcpy(&temp, buffer, LIGHTING_PAYLOAD_SIZE);

    if (temp.command_id < CMD_LIGHTING_SET_PATTERN || temp.command_id > CMD_LIGHTING_SET_BRIGHTNESS) {
        return PROTOCOL_ERROR_INVALID_CMD;
    }
    if (temp.pattern_id > PATTERN_TWINKLE) {
        return PROTOCOL_ERROR_INVALID_PARAM;
    }
    if (temp.brightness > 100) {
        return PROTOCOL_ERROR_INVALID_PARAM;
    }

    *out_payload = temp;
    return PROTOCOL_OK;
}

protocol_status_t parse_audio_control_frame(const uint8_t *buffer, size_t size, audio_control_payload_t *out_payload) {
    if (buffer == NULL || out_payload == NULL) {
        return PROTOCOL_ERROR_NULL_POINTER;
    }
    if (size != AUDIO_PAYLOAD_SIZE) {
        return PROTOCOL_ERROR_INVALID_SIZE;
    }

    audio_control_payload_t temp;
    memcpy(&temp, buffer, AUDIO_PAYLOAD_SIZE);

    if (temp.command_id < CMD_AUDIO_PLAY_SONG || temp.command_id > CMD_AUDIO_SET_VOLUME) {
        return PROTOCOL_ERROR_INVALID_CMD;
    }
    if (temp.melody_id > MELODY_DECK_THE_HALLS) {
        return PROTOCOL_ERROR_INVALID_PARAM;
    }
    if (temp.volume > 100) {
        return PROTOCOL_ERROR_INVALID_PARAM;
    }

    *out_payload = temp;
    return PROTOCOL_OK;
}

protocol_status_t serialize_telemetry_frame(const telemetry_payload_t *payload, uint8_t *out_buffer, size_t buffer_size) {
    if (payload == NULL || out_buffer == NULL) {
        return PROTOCOL_ERROR_NULL_POINTER;
    }
    if (buffer_size < TELEMETRY_PAYLOAD_SIZE) {
        return PROTOCOL_ERROR_INVALID_SIZE;
    }

    memcpy(out_buffer, payload, TELEMETRY_PAYLOAD_SIZE);
    return PROTOCOL_OK;
}

protocol_status_t parse_telemetry_frame(const uint8_t *buffer, size_t size, telemetry_payload_t *out_payload) {
    if (buffer == NULL || out_payload == NULL) {
        return PROTOCOL_ERROR_NULL_POINTER;
    }
    if (size != TELEMETRY_PAYLOAD_SIZE) {
        return PROTOCOL_ERROR_INVALID_SIZE;
    }

    memcpy(out_payload, buffer, TELEMETRY_PAYLOAD_SIZE);
    return PROTOCOL_OK;
}
