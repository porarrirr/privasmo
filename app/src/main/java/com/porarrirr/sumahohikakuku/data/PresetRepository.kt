package com.porarrirr.sumahohikakuku.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.porarrirr.sumahohikakuku.model.PresetDeviceSnapshot
import com.porarrirr.sumahohikakuku.model.PresetSnapshot
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.presetDataStore by preferencesDataStore(name = "sensor_presets")

private const val PRESET_KEY_PREFIX = "preset_"
private val LEGACY_PRESETS_JSON_KEY = stringPreferencesKey("presets_json")

sealed interface PresetRepositoryError {
    data class ReadFailed(val cause: Throwable) : PresetRepositoryError
    data class DecodeFailed(val cause: Throwable) : PresetRepositoryError
    data class WriteFailed(val cause: Throwable) : PresetRepositoryError
}

class PresetRepository(
    private val context: Context,
    private val json: Json = defaultJson
) {
    private val presetSerializer = PresetSnapshot.serializer()

    private val _errors = MutableSharedFlow<PresetRepositoryError>(extraBufferCapacity = 8)
    val errors: SharedFlow<PresetRepositoryError> = _errors.asSharedFlow()

    val presetsFlow: Flow<List<PresetSnapshot>> = context.presetDataStore.data
        .catch { error ->
            _errors.tryEmit(PresetRepositoryError.ReadFailed(error))
            emit(emptyPreferences())
        }
        .map { preferences -> decodePresets(preferences) }
        .distinctUntilChanged()

    suspend fun upsertPreset(preset: PresetSnapshot) {
        try {
            context.presetDataStore.edit { preferences ->
                migrateLegacyIfNeeded(preferences)
                preferences[presetKey(preset.id)] = json.encodeToString(presetSerializer, preset)
            }
        } catch (error: Throwable) {
            _errors.tryEmit(PresetRepositoryError.WriteFailed(error))
            throw error
        }
    }

    suspend fun deletePreset(presetId: String) {
        try {
            context.presetDataStore.edit { preferences ->
                migrateLegacyIfNeeded(preferences)
                preferences.remove(presetKey(presetId))
            }
        } catch (error: Throwable) {
            _errors.tryEmit(PresetRepositoryError.WriteFailed(error))
            throw error
        }
    }

    suspend fun updatePresetName(presetId: String, newName: String) {
        try {
            context.presetDataStore.edit { preferences ->
                migrateLegacyIfNeeded(preferences)
                val key = presetKey(presetId)
                val raw = preferences[key] ?: return@edit
                val current = runCatching { json.decodeFromString(presetSerializer, raw) }
                    .onFailure { error -> _errors.tryEmit(PresetRepositoryError.DecodeFailed(error)) }
                    .getOrNull()
                    ?: return@edit

                val updated = current.copy(
                    name = newName,
                    updatedAtEpochMillis = System.currentTimeMillis()
                )
                preferences[key] = json.encodeToString(presetSerializer, updated)
            }
        } catch (error: Throwable) {
            _errors.tryEmit(PresetRepositoryError.WriteFailed(error))
            throw error
        }
    }

    private fun decodePresets(preferences: Preferences): List<PresetSnapshot> {
        val storedPresets = preferences.asMap()
            .filterKeys { key -> key.name.startsWith(PRESET_KEY_PREFIX) }
            .mapNotNull { (_, rawValue) ->
                val raw = rawValue as? String ?: return@mapNotNull null
                runCatching { json.decodeFromString(presetSerializer, raw) }
                    .onFailure { error -> _errors.tryEmit(PresetRepositoryError.DecodeFailed(error)) }
                    .getOrNull()
            }

        if (storedPresets.isNotEmpty()) {
            return storedPresets.sortedBy { it.name.lowercase(Locale.ROOT) }
        }

        val legacyRaw = preferences[LEGACY_PRESETS_JSON_KEY].orEmpty()
        if (legacyRaw.isBlank()) return emptyList()

        val decoded = runCatching { json.decodeFromString(listSerializer, legacyRaw) }
            .onFailure { error -> _errors.tryEmit(PresetRepositoryError.DecodeFailed(error)) }
            .getOrElse { convertLegacyPresets(legacyRaw) }

        return decoded.sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    private fun migrateLegacyIfNeeded(preferences: MutablePreferences) {
        val legacyRaw = preferences[LEGACY_PRESETS_JSON_KEY].orEmpty()
        if (legacyRaw.isBlank()) {
            preferences.remove(LEGACY_PRESETS_JSON_KEY)
            return
        }

        val hasNewKeys = preferences.asMap().keys.any { key -> key.name.startsWith(PRESET_KEY_PREFIX) }
        if (hasNewKeys) {
            preferences.remove(LEGACY_PRESETS_JSON_KEY)
            return
        }

        val decodedList = runCatching { json.decodeFromString(listSerializer, legacyRaw) }.getOrNull()
        val decoded = decodedList ?: convertLegacyPresets(legacyRaw).takeIf { it.isNotEmpty() } ?: return

        decoded.forEach { preset ->
            preferences[presetKey(preset.id)] = json.encodeToString(presetSerializer, preset)
        }
        preferences.remove(LEGACY_PRESETS_JSON_KEY)
    }

    private fun convertLegacyPresets(raw: String): List<PresetSnapshot> {
        val legacy = runCatching { json.decodeFromString(legacyListSerializer, raw) }
            .onFailure { error -> _errors.tryEmit(PresetRepositoryError.DecodeFailed(error)) }
            .getOrElse { return emptyList() }
        if (legacy.isEmpty()) return emptyList()

        return legacy.flatMap { legacySnapshot ->
            if (legacySnapshot.devices.isEmpty()) return@flatMap emptyList<PresetSnapshot>()
            val deviceCount = legacySnapshot.devices.size
            val baseId = legacySnapshot.id.ifBlank { UUID.randomUUID().toString() }
            legacySnapshot.devices.mapIndexed { index, device ->
                val nameSuffix = if (deviceCount > 1) " (${index + 1})" else ""
                val idSuffix = if (deviceCount > 1) "-${index + 1}-${UUID.randomUUID()}" else ""
                PresetSnapshot(
                    id = baseId + idSuffix,
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

    private fun presetKey(presetId: String) = stringPreferencesKey(PRESET_KEY_PREFIX + encodePreferencesKeyComponent(presetId))
}

@Serializable
private data class LegacyPresetSnapshot(
    val id: String,
    val name: String,
    val devices: List<PresetDeviceSnapshot> = emptyList(),
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L
)
