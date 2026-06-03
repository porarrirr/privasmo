package com.porarrirr.sumahohikakuku.model

import kotlinx.serialization.Serializable

@Serializable
data class PresetLensSnapshot(
    val nativeFocalLength: String,
    val selectedSensorValue: String,
    val manualSensorDescriptor: String,
    val fNumber: String,
    val opticalEndFocalLength: String = "",
    val endFNumber: String = ""
)

@Serializable
data class PresetDeviceSnapshot(
    val name: String,
    val colorHex: String,
    val lenses: List<PresetLensSnapshot>
)

@Serializable
data class PresetSnapshot(
    val id: String,
    val name: String,
    val device: PresetDeviceSnapshot,
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L
)
