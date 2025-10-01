package com.porarrirr.sumahohikakuku.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.porarrirr.sumahohikakuku.model.PresetDeviceSnapshot
import com.porarrirr.sumahohikakuku.model.PresetSnapshot
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

private val Context.presetDataStore by preferencesDataStore(name = "sensor_presets")

private val PRESETS_JSON_KEY = stringPreferencesKey("presets_json")

class PresetRepository(
    private val context: Context,
    private val json: Json = defaultJson
) {

    val presetsFlow: Flow<List<PresetSnapshot>> = context.presetDataStore.data
        .map { preferences ->
            runCatching { decodePresets(preferences) }.getOrDefault(emptyList())
        }

    suspend fun upsertPreset(preset: PresetSnapshot) {
        context.presetDataStore.edit { preferences ->
            val current = decodePresets(preferences)
            val next = current
                .filterNot { it.id == preset.id }
                .toMutableList()
                .also { list -> list.add(preset) }
                .sortedBy { it.name.lowercase(Locale.ROOT) }
            persist(next, preferences)
        }
    }

    suspend fun deletePreset(presetId: String) {
        context.presetDataStore.edit { preferences ->
            val current = decodePresets(preferences)
            val next = current.filterNot { it.id == presetId }
            persist(next, preferences)
        }
    }

    suspend fun updatePresetName(presetId: String, newName: String) {
        context.presetDataStore.edit { preferences ->
            val current = decodePresets(preferences)
            val next = current.map { preset ->
                if (preset.id == presetId) {
                    preset.copy(name = newName, updatedAtEpochMillis = System.currentTimeMillis())
                } else {
                    preset
                }
            }
            persist(next, preferences)
        }
    }

    private fun decodePresets(preferences: Preferences): List<PresetSnapshot> {
        val raw = preferences[PRESETS_JSON_KEY] ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return runCatching { json.decodeFromString(listSerializer, raw) }
            .getOrElse { convertLegacyPresets(raw) }
    }

    private fun persist(presets: List<PresetSnapshot>, preferences: MutablePreferences) {
        if (presets.isEmpty()) {
            preferences.remove(PRESETS_JSON_KEY)
        } else {
            preferences[PRESETS_JSON_KEY] = json.encodeToString(listSerializer, presets)
        }
    }

    private fun convertLegacyPresets(raw: String): List<PresetSnapshot> {
        val legacy = runCatching { json.decodeFromString(legacyListSerializer, raw) }.getOrElse { return emptyList() }
        if (legacy.isEmpty()) return emptyList()
        return legacy.flatMap { legacySnapshot ->
            if (legacySnapshot.devices.isEmpty()) return@flatMap emptyList<PresetSnapshot>()
            val deviceCount = legacySnapshot.devices.size
            val baseId = legacySnapshot.id.ifBlank { UUID.randomUUID().toString() }
            legacySnapshot.devices.mapIndexed { index, device ->
                val nameSuffix = if (deviceCount > 1) " (${index + 1})" else ""
                val idSuffix = if (deviceCount > 1) "-${index + 1}-${UUID.randomUUID()}" else ""
                val resolvedId = baseId + idSuffix
                PresetSnapshot(
                    id = resolvedId,
                    name = legacySnapshot.name + nameSuffix,
                    device = device,
                    createdAtEpochMillis = legacySnapshot.createdAtEpochMillis,
                    updatedAtEpochMillis = legacySnapshot.updatedAtEpochMillis
                )
            }
        }
    }

    companion object {
        private val listSerializer = ListSerializer(PresetSnapshot.serializer())
        private val legacyListSerializer = ListSerializer(LegacyPresetSnapshot.serializer())
        private val defaultJson = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            prettyPrint = false
        }
    }
}

@Serializable
private data class LegacyPresetSnapshot(
    val id: String,
    val name: String,
    val devices: List<PresetDeviceSnapshot> = emptyList(),
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L
)
