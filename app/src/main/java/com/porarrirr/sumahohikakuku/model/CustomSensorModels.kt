package com.porarrirr.sumahohikakuku.model

import kotlinx.serialization.Serializable

@Serializable
data class CustomSensorEntry(
    val id: String,
    val name: String,
    val megapixels: Double,
    val pixelSizeUm: Double,
    val binningType: String
)

fun CustomSensorEntry.toSensorSpec(): SensorSpec {
    return SensorSpec(
        name = name,
        value = id,
        megapixels = megapixels,
        pixelSizeUm = pixelSizeUm,
        binningType = normalizeBinning(binningType),
        manufacturer = detectManufacturer(name),
        source = SensorSource.DATABASE
    )
}
