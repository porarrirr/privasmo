package com.porarrirr.sumahohikakuku.viewmodel

import com.porarrirr.sumahohikakuku.model.SensorSource
import com.porarrirr.sumahohikakuku.model.SensorSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GenerateComparisonUseCaseTest {

    private val sensorSpec = SensorSpec(
        name = "Sony IMX999",
        value = "Sony IMX999",
        megapixels = 50.0,
        pixelSizeUm = 1.0,
        binningType = "None",
        manufacturer = "Sony",
        source = SensorSource.DATABASE
    )

    private val useCase = GenerateComparisonUseCase()
    private val defaultFocalLengths = listOf(14.0, 24.0, 35.0, 50.0, 70.0)

    @Test
    fun generate_returnsNullResultsWhenNoLensIsValid() {
        val devices = listOf(
            DeviceInputState(
                id = 1L,
                name = "Device 1",
                colorHex = "#2563EB",
                lenses = listOf(
                    LensInputState(
                        id = 1L,
                        nativeFocalLength = "invalid",
                        selectedSensorValue = sensorSpec.value,
                        manualSensorDescriptor = "",
                        fNumber = "1.8"
                    )
                )
            )
        )

        val output = useCase.generate(
            devices = devices,
            availableSensors = listOf(sensorSpec),
            selectedFocalLength = 35.0,
            defaultFocalLengths = defaultFocalLengths,
            fallbackDeviceName = { "Device $it" }
        )

        assertNull(output.results)
        assertEquals(defaultFocalLengths, output.focalLengths)
        assertEquals(defaultFocalLengths.first(), output.selectedFocalLength, 0.0)
    }

    @Test
    fun generate_selectsNearestAvailableFocalLength() {
        val devices = listOf(
            DeviceInputState(
                id = 1L,
                name = "Device 1",
                colorHex = "#2563EB",
                lenses = listOf(
                    LensInputState(
                        id = 1L,
                        nativeFocalLength = "24",
                        selectedSensorValue = sensorSpec.value,
                        manualSensorDescriptor = "",
                        fNumber = "1.8"
                    )
                )
            )
        )

        val output = useCase.generate(
            devices = devices,
            availableSensors = listOf(sensorSpec),
            selectedFocalLength = 41.0,
            defaultFocalLengths = defaultFocalLengths,
            fallbackDeviceName = { "Device $it" }
        )

        assertNotNull(output.results)
        assertEquals(35.0, output.selectedFocalLength, 0.0)
    }

    @Test
    fun generate_usesFallbackDeviceNameWhenBlank() {
        val devices = listOf(
            DeviceInputState(
                id = 1L,
                name = "",
                colorHex = "#2563EB",
                lenses = listOf(
                    LensInputState(
                        id = 1L,
                        nativeFocalLength = "24",
                        selectedSensorValue = sensorSpec.value,
                        manualSensorDescriptor = "",
                        fNumber = "1.8"
                    )
                )
            )
        )

        val output = useCase.generate(
            devices = devices,
            availableSensors = listOf(sensorSpec),
            selectedFocalLength = 24.0,
            defaultFocalLengths = defaultFocalLengths,
            fallbackDeviceName = { "Fallback Device $it" }
        )

        val results = output.results
        assertNotNull(results)
        assertEquals("Fallback Device 1", results!!.devices.first().name)
    }
}
