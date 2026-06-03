package com.porarrirr.sumahohikakuku.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.porarrirr.sumahohikakuku.model.MANUAL_INPUT_SENSOR_VALUE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.json.JSONArray

private val Context.deviceInputsDataStore by preferencesDataStore(name = "sensor_comparison_inputs")
private val DEVICES_JSON_KEY = stringPreferencesKey("devices_json")

private const val LEGACY_PREFS_NAME = "sensor_comparison_prefs"
private const val LEGACY_KEY_SAVED_DEVICES = "saved_devices"

@Serializable
data class SavedDeviceInput(
    val name: String = "",
    val colorHex: String = "",
    val lenses: List<SavedLensInput> = emptyList()
)

@Serializable
data class SavedLensInput(
    val nativeFocalLength: String = "",
    val selectedSensorValue: String = MANUAL_INPUT_SENSOR_VALUE,
    val manualSensorDescriptor: String = "",
    val fNumber: String = "",
    val opticalEndFocalLength: String = "",
    val endFNumber: String = ""
)

class DeviceInputRepository(
    private val context: Context,
    private val json: Json = defaultJson,
    private val legacyPreferences: SharedPreferences = context.getSharedPreferences(
        LEGACY_PREFS_NAME,
        Context.MODE_PRIVATE
    )
) {
    private val listSerializer = ListSerializer(SavedDeviceInput.serializer())

    suspend fun load(): Result<List<SavedDeviceInput>?> = withContext(Dispatchers.IO) {
        runCatching {
            val preferences = context.deviceInputsDataStore.data.first()
            val raw = preferences[DEVICES_JSON_KEY].orEmpty()
            if (raw.isNotBlank()) {
                return@runCatching json.decodeFromString(listSerializer, raw).takeIf { it.isNotEmpty() }
            }

            val legacyRaw = legacyPreferences.getString(LEGACY_KEY_SAVED_DEVICES, null)
                ?.takeIf { it.isNotBlank() }
                ?: return@runCatching null

            val migrated = decodeLegacy(legacyRaw).takeIf { it.isNotEmpty() }
            context.deviceInputsDataStore.edit { edited ->
                if (migrated == null) {
                    edited.remove(DEVICES_JSON_KEY)
                } else {
                    edited[DEVICES_JSON_KEY] = json.encodeToString(listSerializer, migrated)
                }
            }
            legacyPreferences.edit().remove(LEGACY_KEY_SAVED_DEVICES).apply()

            migrated
        }
    }

    suspend fun save(devices: List<SavedDeviceInput>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.deviceInputsDataStore.edit { preferences ->
                if (devices.isEmpty()) {
                    preferences.remove(DEVICES_JSON_KEY)
                } else {
                    preferences[DEVICES_JSON_KEY] = json.encodeToString(listSerializer, devices)
                }
            }
            Unit
        }
    }

    private fun decodeLegacy(raw: String): List<SavedDeviceInput> {
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val deviceObject = array.optJSONObject(i) ?: continue
                    val lensArray = deviceObject.optJSONArray("lenses")
                    val lenses = if (lensArray == null) {
                        emptyList()
                    } else {
                        buildList {
                            for (j in 0 until lensArray.length()) {
                                val lensObject = lensArray.optJSONObject(j) ?: continue
                                add(
                                    SavedLensInput(
                                        nativeFocalLength = lensObject.optString("nativeFocalLength").orEmpty(),
                                        selectedSensorValue = lensObject.optString("selectedSensorValue")
                                            .ifBlank { MANUAL_INPUT_SENSOR_VALUE },
                                        manualSensorDescriptor = lensObject.optString("manualSensorDescriptor").orEmpty(),
                                        fNumber = lensObject.optString("fNumber").orEmpty(),
                                        opticalEndFocalLength = lensObject.optString("opticalEndFocalLength").orEmpty(),
                                        endFNumber = lensObject.optString("endFNumber").orEmpty()
                                    )
                                )
                            }
                        }
                    }
                    add(
                        SavedDeviceInput(
                            name = deviceObject.optString("name").orEmpty(),
                            colorHex = deviceObject.optString("colorHex").orEmpty(),
                            lenses = lenses
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    companion object {
        private val defaultJson = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            prettyPrint = false
        }
    }
}
