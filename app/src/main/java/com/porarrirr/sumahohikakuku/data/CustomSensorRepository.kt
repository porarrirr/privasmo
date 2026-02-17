package com.porarrirr.sumahohikakuku.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.porarrirr.sumahohikakuku.model.CustomSensorEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.customSensorDataStore by preferencesDataStore(name = "custom_sensors")
private const val SENSOR_KEY_PREFIX = "custom_sensor_"
private val LEGACY_SENSORS_KEY = stringPreferencesKey("custom_sensors_json")

sealed interface CustomSensorRepositoryError {
    data class ReadFailed(val cause: Throwable) : CustomSensorRepositoryError
    data class DecodeFailed(val cause: Throwable) : CustomSensorRepositoryError
    data class WriteFailed(val cause: Throwable) : CustomSensorRepositoryError
}

class CustomSensorRepository(
    private val context: Context
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    private val entrySerializer = CustomSensorEntry.serializer()
    private val listSerializer = ListSerializer(entrySerializer)

    private val _errors = MutableSharedFlow<CustomSensorRepositoryError>(extraBufferCapacity = 8)
    val errors: SharedFlow<CustomSensorRepositoryError> = _errors.asSharedFlow()

    val sensorsFlow: Flow<List<CustomSensorEntry>> = context.customSensorDataStore.data
        .catch { error ->
            _errors.tryEmit(CustomSensorRepositoryError.ReadFailed(error))
            emit(emptyPreferences())
        }
        .map { preferences -> decodeSensors(preferences) }
        .distinctUntilChanged()

    suspend fun upsertSensor(entry: CustomSensorEntry) {
        runCatching {
            context.customSensorDataStore.edit { preferences ->
                migrateLegacyIfNeeded(preferences)
                preferences[sensorKey(entry.id)] = json.encodeToString(entrySerializer, entry)
            }
        }.onFailure { error ->
            _errors.tryEmit(CustomSensorRepositoryError.WriteFailed(error))
        }
    }

    suspend fun deleteSensor(id: String) {
        runCatching {
            context.customSensorDataStore.edit { preferences ->
                migrateLegacyIfNeeded(preferences)
                preferences.remove(sensorKey(id))
            }
        }.onFailure { error ->
            _errors.tryEmit(CustomSensorRepositoryError.WriteFailed(error))
        }
    }

    private fun decodeSensors(preferences: Preferences): List<CustomSensorEntry> {
        val storedSensors = preferences.asMap()
            .filterKeys { key -> key.name.startsWith(SENSOR_KEY_PREFIX) }
            .mapNotNull { (_, rawValue) ->
                val raw = rawValue as? String ?: return@mapNotNull null
                runCatching { json.decodeFromString(entrySerializer, raw) }
                    .onFailure { error ->
                        _errors.tryEmit(CustomSensorRepositoryError.DecodeFailed(error))
                    }
                    .getOrNull()
            }
        if (storedSensors.isNotEmpty()) return storedSensors.sortedBy { it.name }

        val legacyRaw = preferences[LEGACY_SENSORS_KEY].orEmpty()
        if (legacyRaw.isBlank()) return emptyList()

        return runCatching { json.decodeFromString(listSerializer, legacyRaw) }
            .onFailure { error -> _errors.tryEmit(CustomSensorRepositoryError.DecodeFailed(error)) }
            .getOrElse { emptyList() }
            .sortedBy { it.name }
    }

    private fun migrateLegacyIfNeeded(preferences: MutablePreferences) {
        val legacyRaw = preferences[LEGACY_SENSORS_KEY].orEmpty()
        if (legacyRaw.isBlank()) return

        val hasNewKeys = preferences.asMap().keys.any { key -> key.name.startsWith(SENSOR_KEY_PREFIX) }
        if (hasNewKeys) {
            preferences.remove(LEGACY_SENSORS_KEY)
            return
        }

        val decoded = runCatching { json.decodeFromString(listSerializer, legacyRaw) }
            .onFailure { error -> _errors.tryEmit(CustomSensorRepositoryError.DecodeFailed(error)) }
            .getOrNull()
            ?: return

        decoded.forEach { entry ->
            preferences[sensorKey(entry.id)] = json.encodeToString(entrySerializer, entry)
        }
        preferences.remove(LEGACY_SENSORS_KEY)
    }

    private fun sensorKey(id: String) = stringPreferencesKey(SENSOR_KEY_PREFIX + encodePreferencesKeyComponent(id))
}
