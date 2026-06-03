package com.porarrirr.sumahohikakuku.viewmodel

import com.porarrirr.sumahohikakuku.model.MANUAL_INPUT_SENSOR_VALUE
import com.porarrirr.sumahohikakuku.model.SensorSource
import com.porarrirr.sumahohikakuku.model.SensorSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GenerateComparisonUseCaseTest {

    private val useCase = GenerateComparisonUseCase()
    private val defaultFocalLengths = listOf(14.0, 24.0, 35.0, 50.0, 70.0)
    private val databaseSensor = SensorSpec(
        name = "Sony IMX999",
        value = "sensor:sony-imx999",
        megapixels = 50.0,
        pixelSizeUm = 1.0,
        binningType = "None",
        manufacturer = "Sony",
        source = SensorSource.DATABASE
    )

    @Test
    fun generate_buildsFocalGridFromReachableNativeAndOpticalEndFocals() {
        val output = useCase.generate(
            devices = listOf(
                device(
                    lenses = listOf(
                        lens(
                            nativeFocalLength = "75",
                            fNumber = "2.39",
                            opticalEndFocalLength = "100",
                            endFNumber = "2.96"
                        )
                    )
                )
            ),
            availableSensors = listOf(databaseSensor),
            selectedFocalLength = 90.0,
            defaultFocalLengths = defaultFocalLengths,
            fallbackDeviceName = { "Device $it" }
        )

        assertNotNull(output.results)
        assertEquals(listOf(75.0, 100.0), output.focalLengths)
        assertEquals(listOf(75.0, 100.0), output.results!!.focalLengths)
        assertEquals(100.0, output.selectedFocalLength, 0.0)
    }

    @Test
    fun generate_processesValidLensesWhenOtherLensInputsAreInvalid() {
        val output = useCase.generate(
            devices = listOf(
                device(
                    name = "",
                    lenses = listOf(
                        lens(nativeFocalLength = "bad", fNumber = "1.8"),
                        lens(id = 2L, nativeFocalLength = "24", fNumber = "1.8")
                    )
                )
            ),
            availableSensors = listOf(databaseSensor),
            selectedFocalLength = 35.0,
            defaultFocalLengths = defaultFocalLengths,
            fallbackDeviceName = { "Fallback Device $it" }
        )

        val processedDevice = output.results!!.devices.single()
        assertEquals("Fallback Device 1", processedDevice.name)
        assertEquals(listOf(24.0), processedDevice.lenses.map { it.nativeFocalLength35mm })
        assertEquals(listOf(24.0, 35.0, 50.0, 70.0), output.focalLengths)
        assertEquals(35.0, output.selectedFocalLength, 0.0)
    }

    @Test
    fun generate_usesManualDescriptorOnlyForManualSensorSelections() {
        val output = useCase.generate(
            devices = listOf(
                device(
                    lenses = listOf(
                        lens(
                            id = 1L,
                            nativeFocalLength = "24",
                            selectedSensorValue = databaseSensor.value,
                            manualSensorDescriptor = "not a descriptor",
                            fNumber = "1.8"
                        ),
                        lens(
                            id = 2L,
                            nativeFocalLength = "50",
                            selectedSensorValue = MANUAL_INPUT_SENSOR_VALUE,
                            manualSensorDescriptor = "1/1.33",
                            fNumber = "2.0"
                        )
                    )
                )
            ),
            availableSensors = listOf(databaseSensor),
            selectedFocalLength = 24.0,
            defaultFocalLengths = listOf(24.0, 50.0),
            fallbackDeviceName = { "Device $it" }
        )

        val lenses = output.results!!.devices.single().lenses
        assertEquals("Sony IMX999", lenses[0].sensorMetrics.sensorName)
        assertEquals("1/1.33", lenses[1].sensorMetrics.sensorName)
    }

    @Test
    fun generate_returnsDefaultFocalGridWhenNoDeviceCanBeProcessed() {
        val output = useCase.generate(
            devices = listOf(
                device(lenses = listOf(lens(nativeFocalLength = "bad", fNumber = "1.8")))
            ),
            availableSensors = listOf(databaseSensor),
            selectedFocalLength = 35.0,
            defaultFocalLengths = defaultFocalLengths,
            fallbackDeviceName = { "Device $it" }
        )

        assertNull(output.results)
        assertEquals(defaultFocalLengths, output.focalLengths)
        assertEquals(defaultFocalLengths.first(), output.selectedFocalLength, 0.0)
    }

    @Test
    fun generate_rejectsManualSensorWithMalformedDescriptor() {
        val output = useCase.generate(
            devices = listOf(
                device(
                    lenses = listOf(
                        lens(
                            selectedSensorValue = MANUAL_INPUT_SENSOR_VALUE,
                            manualSensorDescriptor = "1/1.28/2"
                        )
                    )
                )
            ),
            availableSensors = listOf(databaseSensor),
            selectedFocalLength = 24.0,
            defaultFocalLengths = defaultFocalLengths,
            fallbackDeviceName = { "Device $it" }
        )

        assertNull(output.results)
    }

    @Test
    fun generate_rejectsEndFNumberWithoutOpticalEndFocalLength() {
        val output = useCase.generate(
            devices = listOf(
                device(lenses = listOf(lens(opticalEndFocalLength = "", endFNumber = "2.96")))
            ),
            availableSensors = listOf(databaseSensor),
            selectedFocalLength = 24.0,
            defaultFocalLengths = defaultFocalLengths,
            fallbackDeviceName = { "Device $it" }
        )

        assertNull(output.results)
    }

    private fun device(
        id: Long = 1L,
        name: String = "Device $id",
        colorHex: String = "#2563EB",
        lenses: List<LensInputState> = listOf(lens())
    ): DeviceInputState {
        return DeviceInputState(
            id = id,
            name = name,
            colorHex = colorHex,
            lenses = lenses
        )
    }

    private fun lens(
        id: Long = 1L,
        nativeFocalLength: String = "24",
        selectedSensorValue: String = databaseSensor.value,
        manualSensorDescriptor: String = "",
        fNumber: String = "1.8",
        opticalEndFocalLength: String = "",
        endFNumber: String = ""
    ): LensInputState {
        return LensInputState(
            id = id,
            nativeFocalLength = nativeFocalLength,
            selectedSensorValue = selectedSensorValue,
            manualSensorDescriptor = manualSensorDescriptor,
            fNumber = fNumber,
            opticalEndFocalLength = opticalEndFocalLength,
            endFNumber = endFNumber
        )
    }
}
