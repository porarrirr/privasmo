package com.porarrirr.sumahohikakuku.viewmodel

import com.porarrirr.sumahohikakuku.model.MANUAL_INPUT_SENSOR_VALUE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpticalLensInputTest {

    @Test
    fun parseOptionalOpticalLensInput_returnsFixedLensWhenOptionalFieldsAreBlank() {
        val parsed = parseOptionalOpticalLensInput(
            lens = lens(opticalEndFocalLength = "", endFNumber = ""),
            nativeFocal = 24.0,
            fNumber = 1.8
        )

        assertEquals(24.0, parsed!!.endFocal, 0.0)
        assertEquals(1.8, parsed.endFNumber, 0.0)
    }

    @Test
    fun parseOptionalOpticalLensInput_defaultsEndFNumberToStartWhenBlank() {
        val parsed = parseOptionalOpticalLensInput(
            lens = lens(opticalEndFocalLength = "100", endFNumber = ""),
            nativeFocal = 75.0,
            fNumber = 2.39
        )

        assertEquals(100.0, parsed!!.endFocal, 0.0)
        assertEquals(2.39, parsed.endFNumber, 0.0)
    }

    @Test
    fun parseOptionalOpticalLensInput_parsesValidOpticalZoomEnd() {
        val parsed = parseOptionalOpticalLensInput(
            lens = lens(opticalEndFocalLength = "100", endFNumber = "2.96"),
            nativeFocal = 75.0,
            fNumber = 2.39
        )

        assertEquals(100.0, parsed!!.endFocal, 0.0)
        assertEquals(2.96, parsed.endFNumber, 0.0)
    }

    @Test
    fun parseOptionalOpticalLensInput_rejectsEndFNumberWithoutEndFocal() {
        val parsed = parseOptionalOpticalLensInput(
            lens = lens(opticalEndFocalLength = "", endFNumber = "2.96"),
            nativeFocal = 75.0,
            fNumber = 2.39
        )

        assertNull(parsed)
    }

    @Test
    fun parseOptionalOpticalLensInput_rejectsInvalidEndFocalOrEndFNumber() {
        val invalidLenses = listOf(
            lens(opticalEndFocalLength = "74.9", endFNumber = "2.96"),
            lens(opticalEndFocalLength = "abc", endFNumber = "2.96"),
            lens(opticalEndFocalLength = "100", endFNumber = "0"),
            lens(opticalEndFocalLength = "100", endFNumber = "-2.96"),
            lens(opticalEndFocalLength = "100", endFNumber = "abc")
        )

        invalidLenses.forEach { invalid ->
            assertNull(parseOptionalOpticalLensInput(invalid, nativeFocal = 75.0, fNumber = 2.39))
        }
    }

    private fun lens(
        opticalEndFocalLength: String,
        endFNumber: String
    ): LensInputState {
        return LensInputState(
            id = 1L,
            nativeFocalLength = "75",
            selectedSensorValue = MANUAL_INPUT_SENSOR_VALUE,
            manualSensorDescriptor = "1/1.33",
            fNumber = "2.39",
            opticalEndFocalLength = opticalEndFocalLength,
            endFNumber = endFNumber
        )
    }
}
