package com.porarrirr.sumahohikakuku.viewmodel

import com.porarrirr.sumahohikakuku.model.ComparisonResults
import com.porarrirr.sumahohikakuku.model.LensProcessingInput
import com.porarrirr.sumahohikakuku.model.SensorSpec
import com.porarrirr.sumahohikakuku.model.calculateNativeSensorMetrics
import com.porarrirr.sumahohikakuku.model.computeProcessedDevice
import kotlin.math.abs

internal data class ParsedOpticalLensInput(
    val endFocal: Double,
    val endFNumber: Double
)

internal fun parseOptionalOpticalLensInput(
    lens: LensInputState,
    nativeFocal: Double,
    fNumber: Double
): ParsedOpticalLensInput? {
    val endFocalText = lens.opticalEndFocalLength.trim()
    val endFNumberText = lens.endFNumber.trim()
    if (endFocalText.isBlank()) {
        return if (endFNumberText.isBlank()) {
            ParsedOpticalLensInput(endFocal = nativeFocal, endFNumber = fNumber)
        } else {
            null
        }
    }

    val endFocal = endFocalText.toDoubleOrNull()?.takeIf { it >= nativeFocal } ?: return null
    val endFNumber = if (endFNumberText.isBlank()) {
        fNumber
    } else {
        endFNumberText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
    }
    return ParsedOpticalLensInput(endFocal = endFocal, endFNumber = endFNumber)
}

data class GeneratedComparison(
    val results: ComparisonResults?,
    val focalLengths: List<Double>,
    val selectedFocalLength: Double
)

class GenerateComparisonUseCase {
    fun generate(
        devices: List<DeviceInputState>,
        availableSensors: List<SensorSpec>,
        selectedFocalLength: Double,
        defaultFocalLengths: List<Double>,
        fallbackDeviceName: (Int) -> String
    ): GeneratedComparison {
        val sensorLookup = availableSensors.associateBy { it.value }
        val nativeFocals = devices.flatMap { device ->
            device.lenses.flatMap lensFocals@ { lens ->
                val focal = lens.nativeFocalLength.toDoubleOrNull()?.takeIf { it > 0.0 }
                    ?: return@lensFocals emptyList()
                val fNumber = lens.fNumber.toDoubleOrNull()?.takeIf { it > 0.0 }
                    ?: return@lensFocals listOf(focal)
                val opticalEnd = parseOptionalOpticalLensInput(lens, focal, fNumber)
                    ?.endFocal
                    ?.takeIf { it != focal }
                listOfNotNull(
                    focal,
                    opticalEnd
                )
            }
        }
        val focalGrid = (defaultFocalLengths + nativeFocals).distinct().sorted()

        val processedDevices = devices.mapIndexedNotNull { index, device ->
            val sanitizedName = device.name.ifBlank { fallbackDeviceName(index + 1) }
            val rawLenses = device.lenses.mapNotNull { lens ->
                val focal = lens.nativeFocalLength.toDoubleOrNull()
                val fNumber = lens.fNumber.toDoubleOrNull()
                if (focal == null || focal <= 0.0 || fNumber == null || fNumber <= 0.0) return@mapNotNull null
                val sensorSpec = sensorLookup[lens.selectedSensorValue]
                val manualDescriptor = if (lens.usesManualSensor) lens.manualSensorDescriptor else null
                val metrics = calculateNativeSensorMetrics(sensorSpec, manualDescriptor)
                if (metrics.areaSqMm <= 0.0 || metrics.diagonalMm <= 0.0) return@mapNotNull null
                val optical = parseOptionalOpticalLensInput(lens, focal, fNumber)
                    ?: return@mapNotNull null
                LensProcessingInput(
                    nativeFocalLength35mm = focal,
                    fNumber = fNumber,
                    sensorMetrics = metrics,
                    opticalEndFocalLength35mm = optical.endFocal,
                    endFNumber = optical.endFNumber
                )
            }
            computeProcessedDevice(sanitizedName, device.colorHex, rawLenses, focalGrid)
        }

        if (processedDevices.isEmpty()) {
            return GeneratedComparison(
                results = null,
                focalLengths = defaultFocalLengths,
                selectedFocalLength = defaultFocalLengths.first()
            )
        }

        val availableFocalLengths = processedDevices
            .flatMap { device -> device.metricsByFocalLength.map { it.focalLength35mm } }
            .distinct()
            .sorted()

        if (availableFocalLengths.isEmpty()) {
            return GeneratedComparison(
                results = null,
                focalLengths = defaultFocalLengths,
                selectedFocalLength = defaultFocalLengths.first()
            )
        }

        val nearestFocal = availableFocalLengths.minByOrNull { abs(it - selectedFocalLength) }
            ?: availableFocalLengths.first()

        return GeneratedComparison(
            results = ComparisonResults(
                focalLengths = availableFocalLengths,
                devices = processedDevices
            ),
            focalLengths = availableFocalLengths,
            selectedFocalLength = nearestFocal
        )
    }
}
