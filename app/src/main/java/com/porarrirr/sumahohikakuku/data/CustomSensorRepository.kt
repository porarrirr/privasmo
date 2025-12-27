package com.porarrirr.sumahohikakuku.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.porarrirr.sumahohikakuku.model.CustomSensorEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.customSensorDataStore by preferencesDataStore(name = "custom_sensors")
private val CUSTOM_SENSORS_KEY = stringPreferencesKey("custom_sensors_json")

class CustomSensorRepository(
    private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(CustomSensorEntry.serializer())

    val sensorsFlow: Flow<List<CustomSensorEntry>> = context.customSensorDataStore.data
        .map { preferences ->
            val raw = preferences[CUSTOM_SENSORS_KEY].orEmpty()
            if (raw.isBlank()) {
                emptyList()
            } else {
                runCatching {
                    json.decodeFromString(listSerializer, raw)
                }.getOrElse { emptyList() }
            }
        }
        .distinctUntilChanged()

    suspend fun upsertSensor(entry: CustomSensorEntry) {
        context.customSensorDataStore.edit { preferences ->
            val current = preferences[CUSTOM_SENSORS_KEY].orEmpty()
            val list = if (current.isBlank()) {
                emptyList()
            } else {
                runCatching {
                    json.decodeFromString(listSerializer, current)
                }.getOrElse { emptyList() }
            }
            val updated = list.toMutableList()
            val index = updated.indexOfFirst { it.id == entry.id }
            if (index >= 0) {
                updated[index] = entry
            } else {
                updated.add(entry)
            }
            preferences[CUSTOM_SENSORS_KEY] = json.encodeToString(listSerializer, updated)
        }
    }

    suspend fun deleteSensor(id: String) {
        context.customSensorDataStore.edit { preferences ->
            val current = preferences[CUSTOM_SENSORS_KEY].orEmpty()
            if (current.isBlank()) return@edit
            val list = runCatching {
                json.decodeFromString(listSerializer, current)
            }.getOrElse { emptyList() }
            val updated = list.filterNot { it.id == id }
            if (updated.isEmpty()) {
                preferences.remove(CUSTOM_SENSORS_KEY)
            } else {
                preferences[CUSTOM_SENSORS_KEY] = json.encodeToString(listSerializer, updated)
            }
        }
    }
}
