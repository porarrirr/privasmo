package com.porarrirr.sumahohikakuku.viewmodel

import com.porarrirr.sumahohikakuku.model.ComparisonResults
import com.porarrirr.sumahohikakuku.model.SensorSpec
import com.porarrirr.sumahohikakuku.model.calculateNativeSensorMetrics
import com.porarrirr.sumahohikakuku.model.computeProcessedDevice
import kotlin.math.abs

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
            device.lenses.mapNotNull { lens ->
                lens.nativeFocalLength.toDoubleOrNull()?.takeIf { it > 0.0 }
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
                focal to (fNumber to metrics)
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
