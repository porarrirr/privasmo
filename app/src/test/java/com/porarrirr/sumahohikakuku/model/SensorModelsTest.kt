package com.porarrirr.sumahohikakuku.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class SensorModelsTest {

    @Test
    fun isValidManualSensorDescriptor_acceptsCommaDecimal() {
        assertTrue(isValidManualSensorDescriptor("1/1,33"))
    }

    @Test
    fun isValidManualSensorDescriptor_acceptsFullWidthDigitsAndSlash() {
        assertTrue(isValidManualSensorDescriptor("１／１．３３"))
    }

    @Test
    fun calculateNativeSensorMetrics_handlesCommaDecimal() {
        val metrics = calculateNativeSensorMetrics(sensorSpec = null, manualDescriptor = "1/1,33")

        assertTrue(metrics.areaSqMm > 0.0)
        assertEquals("1/1,33", metrics.sensorName)
    }

    @Test
    fun calculateNativeSensorMetrics_handlesFullWidthDigitsAndPreservesLabel() {
        val metrics = calculateNativeSensorMetrics(sensorSpec = null, manualDescriptor = "１／１．３３")

        assertTrue(metrics.areaSqMm > 0.0)
        assertEquals("１／１．３３", metrics.sensorName)
    }

    @Test
    fun normalizeBinning_isLocaleIndependent() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("16-cell (4x4)", normalizeBinning("16-IN-1"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun detectManufacturer_isLocaleIndependent() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("OmniVision", detectManufacturer("OMNIVISION OV50"))
        } finally {
            Locale.setDefault(original)
        }
    }
}
