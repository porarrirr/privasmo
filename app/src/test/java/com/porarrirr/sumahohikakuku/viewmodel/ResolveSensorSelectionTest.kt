package com.porarrirr.sumahohikakuku.viewmodel

import com.porarrirr.sumahohikakuku.model.MANUAL_INPUT_SENSOR_VALUE
import com.porarrirr.sumahohikakuku.model.SensorSource
import com.porarrirr.sumahohikakuku.model.SensorSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveSensorSelectionTest {

    private val sensor = SensorSpec(
        name = "Sony IMX999",
        value = "sensor:sony-imx999",
        megapixels = 50.0,
        pixelSizeUm = 1.0,
        binningType = "None",
        manufacturer = "Sony",
        source = SensorSource.DATABASE
    )

    @Test
    fun resolveSensorSelection_returnsExistingSensorByValueAndClearsManualDescriptor() {
        val resolved = resolveSensorSelection(
            rawValue = sensor.value,
            manualDescriptor = "1/1.33",
            sensorLookup = mapOf(sensor.value to sensor)
        )

        assertEquals(sensor.value, resolved.value)
        assertNull(resolved.manualDescriptor)
    }

    @Test
    fun resolveSensorSelection_matchesSavedSensorNameIgnoringCase() {
        val resolved = resolveSensorSelection(
            rawValue = "sony imx999",
            manualDescriptor = "",
            sensorLookup = mapOf(sensor.value to sensor)
        )

        assertEquals(sensor.value, resolved.value)
        assertNull(resolved.manualDescriptor)
    }

    @Test
    fun resolveSensorSelection_manualSentinelNormalizesBlankDescriptor() {
        val resolved = resolveSensorSelection(
            rawValue = MANUAL_INPUT_SENSOR_VALUE,
            manualDescriptor = " ",
            sensorLookup = emptyMap()
        )

        assertEquals(MANUAL_INPUT_SENSOR_VALUE, resolved.value)
        assertEquals("1/1.33", resolved.manualDescriptor)
    }

    @Test
    fun resolveSensorSelection_missingSavedValuePreservesLegacyLabelAsManualDescriptor() {
        val resolved = resolveSensorSelection(
            rawValue = "Legacy Sensor X",
            manualDescriptor = "",
            sensorLookup = emptyMap()
        )

        assertEquals(MANUAL_INPUT_SENSOR_VALUE, resolved.value)
        assertEquals("Legacy Sensor X", resolved.manualDescriptor)
    }

    @Test
    fun resolveSensorSelection_blankMissingValueUsesDefaultManualDescriptor() {
        val resolved = resolveSensorSelection(
            rawValue = " ",
            manualDescriptor = " ",
            sensorLookup = emptyMap()
        )

        assertEquals(MANUAL_INPUT_SENSOR_VALUE, resolved.value)
        assertEquals("1/1.33", resolved.manualDescriptor)
    }
}
