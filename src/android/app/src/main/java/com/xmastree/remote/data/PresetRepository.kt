package com.xmastree.remote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xmastree.remote.protocol.MelodyId
import com.xmastree.remote.protocol.PatternId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.presetDataStore: DataStore<Preferences> by preferencesDataStore(name = "xmas_tree_presets")

data class TreePreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val patternId: PatternId,
    val red: Int,
    val green: Int,
    val blue: Int,
    val brightness: Int,
    val speed: Int,
    val melodyId: MelodyId,
    val volume: Int,
    val tempo: Int,
    val paletteId: Int = 0
)

interface PresetStore {
    val presets: Flow<List<TreePreset>>
    suspend fun savePreset(preset: TreePreset)
    suspend fun deletePreset(presetId: String)
}

class PresetRepository(private val context: Context) : PresetStore {

    private val presetsKey = stringPreferencesKey("saved_presets_json")

    override val presets: Flow<List<TreePreset>> = context.presetDataStore.data.map { prefs ->
        val raw = prefs[presetsKey]
        if (raw.isNullOrBlank()) {
            defaultPresets()
        } else {
            decodePresets(raw)
        }
    }

    override suspend fun savePreset(preset: TreePreset) {
        context.presetDataStore.edit { prefs ->
            val current = prefs[presetsKey]?.let { decodePresets(it) } ?: defaultPresets()
            val updated = current.filterNot { it.id == preset.id } + preset
            prefs[presetsKey] = encodePresets(updated)
        }
    }

    override suspend fun deletePreset(presetId: String) {
        context.presetDataStore.edit { prefs ->
            val current = prefs[presetsKey]?.let { decodePresets(it) } ?: defaultPresets()
            val updated = current.filterNot { it.id == presetId }
            prefs[presetsKey] = encodePresets(updated)
        }
    }

    companion object {
        fun defaultPresets(): List<TreePreset> = listOf(
            TreePreset(
                id = "default-classic",
                name = "Classic Christmas",
                patternId = PatternId.FESTIVE_CANDLE,
                red = 255, green = 60, blue = 10,
                brightness = 85, speed = 45,
                melodyId = MelodyId.JINGLE_BELLS,
                volume = 70, tempo = 100
            ),
            TreePreset(
                id = "default-rainbow",
                name = "Rainbow Party",
                patternId = PatternId.RAINBOW_WAVE,
                red = 0, green = 255, blue = 128,
                brightness = 90, speed = 75,
                melodyId = MelodyId.DECK_THE_HALLS,
                volume = 75, tempo = 110
            ),
            TreePreset(
                id = "default-silent-night",
                name = "Cozy Silent Night",
                patternId = PatternId.TWINKLE,
                red = 255, green = 200, blue = 100,
                brightness = 55, speed = 30,
                melodyId = MelodyId.SILENT_NIGHT,
                volume = 50, tempo = 95
            )
        )

        fun encodePresets(list: List<TreePreset>): String {
            val array = JSONArray()
            list.forEach { p ->
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("patternId", p.patternId.id.toInt() and 0xFF)
                    put("red", p.red)
                    put("green", p.green)
                    put("blue", p.blue)
                    put("brightness", p.brightness)
                    put("speed", p.speed)
                    put("melodyId", p.melodyId.id.toInt() and 0xFF)
                    put("volume", p.volume)
                    put("tempo", p.tempo)
                    put("paletteId", p.paletteId)
                }
                array.put(obj)
            }
            return array.toString()
        }

        fun decodePresets(json: String): List<TreePreset> {
            return try {
                val array = JSONArray(json)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val pattern = PatternId.fromByte(obj.optInt("patternId", 0).toByte()) ?: PatternId.OFF
                        val melody = MelodyId.fromByte(obj.optInt("melodyId", 0).toByte()) ?: MelodyId.SILENT
                        add(
                            TreePreset(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                patternId = pattern,
                                red = obj.optInt("red", 255).coerceIn(0, 255),
                                green = obj.optInt("green", 255).coerceIn(0, 255),
                                blue = obj.optInt("blue", 255).coerceIn(0, 255),
                                brightness = obj.optInt("brightness", 80).coerceIn(0, 100),
                                speed = obj.optInt("speed", 50).coerceIn(1, 100),
                                melodyId = melody,
                                volume = obj.optInt("volume", 50).coerceIn(0, 100),
                                tempo = obj.optInt("tempo", 100).coerceIn(50, 200),
                                paletteId = obj.optInt("paletteId", 0).coerceIn(0, 9)
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                defaultPresets()
            }
        }
    }
}
