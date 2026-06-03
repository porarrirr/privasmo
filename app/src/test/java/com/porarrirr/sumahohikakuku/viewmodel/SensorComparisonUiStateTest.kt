package com.porarrirr.sumahohikakuku.viewmodel

import com.porarrirr.sumahohikakuku.model.DEFAULT_DEVICE_COLORS
import com.porarrirr.sumahohikakuku.model.MAX_DEVICES
import com.porarrirr.sumahohikakuku.model.MANUAL_INPUT_SENSOR_VALUE
import com.porarrirr.sumahohikakuku.model.PresetDeviceSnapshot
import com.porarrirr.sumahohikakuku.model.PresetLensSnapshot
import com.porarrirr.sumahohikakuku.model.PresetSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorComparisonUiStateTest {

    @Test
    fun isGenerateEnabled_acceptsAnyCompleteValidLens() {
        val state = SensorComparisonUiState(
            devices = listOf(
                device(id = 1L, lenses = listOf(lens(nativeFocalLength = "bad"))),
                device(id = 2L, lenses = listOf(lens(selectedSensorValue = "database-sensor")))
            )
        )

        assertTrue(state.isGenerateEnabled)
    }

    @Test
    fun isGenerateEnabled_rejectsInvalidManualDescriptor() {
        val state = SensorComparisonUiState(
            devices = listOf(
                device(
                    lenses = listOf(
                        lens(
                            selectedSensorValue = MANUAL_INPUT_SENSOR_VALUE,
                            manualSensorDescriptor = "bad descriptor"
                        )
                    )
                )
            )
        )

        assertFalse(state.isGenerateEnabled)
    }

    @Test
    fun isGenerateEnabled_rejectsEndFNumberWithoutOpticalEndFocalLength() {
        val state = SensorComparisonUiState(
            devices = listOf(
                device(lenses = listOf(lens(opticalEndFocalLength = "", endFNumber = "2.96")))
            )
        )

        assertFalse(state.isGenerateEnabled)
    }

    @Test
    fun canAddDevice_reflectsMaximumDeviceLimit() {
        val fourDevices = (1 until MAX_DEVICES).map { index -> device(id = index.toLong()) }
        val fiveDevices = (1..MAX_DEVICES).map { index -> device(id = index.toLong()) }

        assertTrue(SensorComparisonUiState(devices = fourDevices).canAddDevice)
        assertFalse(SensorComparisonUiState(devices = fiveDevices).canAddDevice)
    }

    @Test
    fun presetListItems_normalizesInvalidColorsAndUsesPresetNameForBlankDeviceName() {
        val state = SensorComparisonUiState(
            presets = listOf(
                PresetSnapshot(
                    id = "preset-1",
                    name = "Travel Kit",
                    device = PresetDeviceSnapshot(
                        name = "",
                        colorHex = "not-a-color",
                        lenses = listOf(
                            PresetLensSnapshot(
                                nativeFocalLength = "24",
                                selectedSensorValue = MANUAL_INPUT_SENSOR_VALUE,
                                manualSensorDescriptor = "1/1.33",
                                fNumber = "1.8"
                            )
                        )
                    ),
                    updatedAtEpochMillis = 123L
                )
            )
        )

        val item = state.presetListItems.single()

        assertEquals("preset-1", item.id)
        assertEquals("Travel Kit", item.name)
        assertEquals("Travel Kit", item.deviceName)
        assertEquals(1, item.lensCount)
        assertEquals(DEFAULT_DEVICE_COLORS.first(), item.colorHex)
        assertEquals(123L, item.updatedAtEpochMillis)
    }

    @Test
    fun isPresetSaveEnabled_requiresNameAndTargetDeviceWithLenses() {
        val targetDevice = device(id = 1L, lenses = listOf(lens()))

        assertFalse(
            SensorComparisonUiState(
                devices = listOf(targetDevice),
                presetTargetDeviceId = targetDevice.id,
                presetNameInput = " "
            ).isPresetSaveEnabled
        )
        assertFalse(
            SensorComparisonUiState(
                devices = listOf(targetDevice.copy(lenses = emptyList())),
                presetTargetDeviceId = targetDevice.id,
                presetNameInput = "Preset"
            ).isPresetSaveEnabled
        )
        assertTrue(
            SensorComparisonUiState(
                devices = listOf(targetDevice),
                presetTargetDeviceId = targetDevice.id,
                presetNameInput = "Preset"
            ).isPresetSaveEnabled
        )
    }

    private fun device(
        id: Long = 1L,
        lenses: List<LensInputState> = listOf(lens())
    ): DeviceInputState {
        return DeviceInputState(
            id = id,
            name = "Device $id",
            colorHex = DEFAULT_DEVICE_COLORS.first(),
            lenses = lenses
        )
    }

    private fun lens(
        id: Long = 1L,
        nativeFocalLength: String = "24",
        selectedSensorValue: String = MANUAL_INPUT_SENSOR_VALUE,
        manualSensorDescriptor: String = "1/1.33",
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
