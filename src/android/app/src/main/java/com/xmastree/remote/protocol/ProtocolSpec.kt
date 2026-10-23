package com.xmastree.remote.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class LightingCommand(val id: Byte) {
    SET_PATTERN(0x01.toByte()),
    SET_STATIC_COLOR(0x02.toByte()),
    SET_BRIGHTNESS(0x03.toByte());

    companion object {
        fun fromByte(b: Byte): LightingCommand? = values().firstOrNull { it.id == b }
    }
}

enum class PatternId(val id: Byte) {
    OFF(0.toByte()),
    STATIC(1.toByte()),
    RAINBOW_WAVE(2.toByte()),
    SPARKLE(3.toByte()),
    FESTIVE_CANDLE(4.toByte()),
    TWINKLE(5.toByte());

    companion object {
        fun fromByte(b: Byte): PatternId? = values().firstOrNull { it.id == b }
    }
}

enum class AudioCommand(val id: Byte) {
    PLAY_SONG(0x10.toByte()),
    STOP_AUDIO(0x11.toByte()),
    PLAY_FREQ_TONE(0x12.toByte()),
    SET_VOLUME(0x13.toByte());

    companion object {
        fun fromByte(b: Byte): AudioCommand? = values().firstOrNull { it.id == b }
    }
}

enum class MelodyId(val id: Byte) {
    SILENT(0.toByte()),
    JINGLE_BELLS(1.toByte()),
    WE_WISH_YOU(2.toByte()),
    SILENT_NIGHT(3.toByte()),
    DECK_THE_HALLS(4.toByte());

    companion object {
        fun fromByte(b: Byte): MelodyId? = values().firstOrNull { it.id == b }
    }
}

enum class PlaybackState(val id: Byte) {
    STOP(0x00.toByte()),
    PLAY(0x01.toByte()),
    PAUSE(0x02.toByte());

    companion object {
        fun fromByte(b: Byte): PlaybackState? = values().firstOrNull { it.id == b }
    }
}

enum class ErrorCode(val id: Byte) {
    OK(0x00.toByte()),
    NVS_ERROR(0x01.toByte()),
    LED_TIMING_ERROR(0x02.toByte());

    companion object {
        fun fromByte(b: Byte): ErrorCode? = values().firstOrNull { it.id == b }
    }
}

data class LightingControlFrame(
    val command: LightingCommand,
    val patternId: PatternId,
    val red: Int,
    val green: Int,
    val blue: Int,
    val brightness: Int,
    val speed: Int,
    val reserved: Int = 0
) {
    init {
        require(red in 0..255) { "Red component must be 0-255" }
        require(green in 0..255) { "Green component must be 0-255" }
        require(blue in 0..255) { "Blue component must be 0-255" }
        require(brightness in 0..100) { "Brightness must be 0-100" }
        require(speed in 1..100) { "Speed must be 1-100" }
    }

    fun toByteArray(): ByteArray {
        return byteArrayOf(
            command.id,
            patternId.id,
            red.toByte(),
            green.toByte(),
            blue.toByte(),
            brightness.toByte(),
            speed.toByte(),
            reserved.toByte()
        )
    }

    companion object {
        const val FRAME_SIZE = 8

        fun fromByteArray(bytes: ByteArray): LightingControlFrame {
            require(bytes.size == FRAME_SIZE) { "Lighting frame must be exactly $FRAME_SIZE bytes" }
            val command = LightingCommand.fromByte(bytes[0])
                ?: throw IllegalArgumentException("Invalid LightingCommand: ${bytes[0]}")
            val patternId = PatternId.fromByte(bytes[1])
                ?: throw IllegalArgumentException("Invalid PatternId: ${bytes[1]}")

            return LightingControlFrame(
                command = command,
                patternId = patternId,
                red = bytes[2].toInt() and 0xFF,
                green = bytes[3].toInt() and 0xFF,
                blue = bytes[4].toInt() and 0xFF,
                brightness = bytes[5].toInt() and 0xFF,
                speed = bytes[6].toInt() and 0xFF,
                reserved = bytes[7].toInt() and 0xFF
            )
        }
    }
}

data class AudioControlFrame(
    val command: AudioCommand,
    val melodyId: MelodyId,
    val playbackState: PlaybackState,
    val volume: Int,
    val tempo: Int,
    val reserved: Int = 0
) {
    init {
        require(volume in 0..100) { "Volume must be 0-100" }
        require(tempo in 50..200) { "Tempo must be 50-200" }
    }

    fun toByteArray(): ByteArray {
        return byteArrayOf(
            command.id,
            melodyId.id,
            playbackState.id,
            volume.toByte(),
            tempo.toByte(),
            reserved.toByte()
        )
    }

    companion object {
        const val FRAME_SIZE = 6

        fun fromByteArray(bytes: ByteArray): AudioControlFrame {
            require(bytes.size == FRAME_SIZE) { "Audio frame must be exactly $FRAME_SIZE bytes" }
            val command = AudioCommand.fromByte(bytes[0])
                ?: throw IllegalArgumentException("Invalid AudioCommand: ${bytes[0]}")
            val melodyId = MelodyId.fromByte(bytes[1])
                ?: throw IllegalArgumentException("Invalid MelodyId: ${bytes[1]}")
            val playbackState = PlaybackState.fromByte(bytes[2])
                ?: throw IllegalArgumentException("Invalid PlaybackState: ${bytes[2]}")

            return AudioControlFrame(
                command = command,
                melodyId = melodyId,
                playbackState = playbackState,
                volume = bytes[3].toInt() and 0xFF,
                tempo = bytes[4].toInt() and 0xFF,
                reserved = bytes[5].toInt() and 0xFF
            )
        }
    }
}

data class TelemetryFrame(
    val systemUptimeSeconds: Long,
    val currentPatternId: PatternId,
    val currentMelodyId: MelodyId,
    val currentBrightness: Int,
    val currentVolume: Int,
    val isConnected: Boolean,
    val errorCode: ErrorCode
) {
    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(FRAME_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(systemUptimeSeconds.toInt())
        buffer.put(currentPatternId.id)
        buffer.put(currentMelodyId.id)
        buffer.put(currentBrightness.toByte())
        buffer.put(currentVolume.toByte())
        buffer.put(if (isConnected) 1.toByte() else 0.toByte())
        buffer.put(errorCode.id)
        return buffer.array()
    }

    companion object {
        const val FRAME_SIZE = 10

        fun fromByteArray(bytes: ByteArray): TelemetryFrame {
            require(bytes.size == FRAME_SIZE) { "Telemetry frame must be exactly $FRAME_SIZE bytes" }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val uptime = buffer.int.toLong() and 0xFFFFFFFFL
            val patternId = PatternId.fromByte(buffer.get())
                ?: throw IllegalArgumentException("Invalid PatternId")
            val melodyId = MelodyId.fromByte(buffer.get())
                ?: throw IllegalArgumentException("Invalid MelodyId")
            val brightness = buffer.get().toInt() and 0xFF
            val volume = buffer.get().toInt() and 0xFF
            val isConnected = buffer.get() != 0.toByte()
            val errorCode = ErrorCode.fromByte(buffer.get())
                ?: throw IllegalArgumentException("Invalid ErrorCode")

            return TelemetryFrame(
                systemUptimeSeconds = uptime,
                currentPatternId = patternId,
                currentMelodyId = melodyId,
                currentBrightness = brightness,
                currentVolume = volume,
                isConnected = isConnected,
                errorCode = errorCode
            )
        }
    }
}
