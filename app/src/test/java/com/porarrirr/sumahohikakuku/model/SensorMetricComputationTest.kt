package com.porarrirr.sumahohikakuku.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorMetricComputationTest {

    @Test
    fun isValidManualSensorDescriptor_acceptsLocalizedFractionFormsAndWhitespace() {
        val validDescriptors = listOf(
            "1/1.33",
            " 1 / 1,33 ",
            "１／１．３３"
        )

        validDescriptors.forEach { descriptor ->
            assertTrue("Expected valid manual descriptor: $descriptor", isValidManualSensorDescriptor(descriptor))
        }
    }

    @Test
    fun isValidManualSensorDescriptor_rejectsBlankMalformedAndZeroFractions() {
        val invalidDescriptors = listOf(
            "",
            "abc/1.28",
            "1//1.28",
            "1/1.28/2",
            "/1.28",
            "1/",
            "0/1.33",
            "1/0"
        )

        invalidDescriptors.forEach { descriptor ->
            assertFalse(
                "Expected invalid manual descriptor: $descriptor",
                isValidManualSensorDescriptor(descriptor)
            )
        }
    }

    @Test
    fun calculateNativeSensorMetrics_databaseSensorCalculatesFourByThreeGeometry() {
        val spec = SensorSpec(
            name = "Sony IMX999",
            value = "Sony IMX999",
            megapixels = 50.0,
            pixelSizeUm = 1.0,
            binningType = "None",
            manufacturer = "Sony",
            source = SensorSource.DATABASE
        )

        val metrics = calculateNativeSensorMetrics(spec, manualDescriptor = "1/1.33")

        assertEquals("Sony IMX999", metrics.sensorName)
        assertEquals(SensorSource.DATABASE, metrics.source)
        assertEquals(50.0, metrics.areaSqMm, 1e-9)
        assertEquals(4.0 / 3.0, metrics.widthMm / metrics.heightMm, 1e-9)
        assertEquals(1.0, metrics.nativePixelSizeUm, 0.0)
    }

    @Test
    fun calculateNativeSensorMetrics_invalidDatabaseSensorReturnsZeroMetrics() {
        val spec = SensorSpec(
            name = "Broken Sensor",
            value = "Broken Sensor",
            megapixels = 0.0,
            pixelSizeUm = 1.0,
            binningType = "None",
            manufacturer = "Other",
            source = SensorSource.DATABASE
        )

        val metrics = calculateNativeSensorMetrics(spec, manualDescriptor = null)

        assertEquals(0.0, metrics.diagonalMm, 0.0)
        assertEquals(0.0, metrics.areaSqMm, 0.0)
        assertEquals("Broken Sensor", metrics.sensorName)
        assertEquals(SensorSource.DATABASE, metrics.source)
    }

    @Test
    fun calculateNativeSensorMetrics_manualDescriptorUsesOpticalFormatFactor() {
        val metrics = calculateNativeSensorMetrics(sensorSpec = null, manualDescriptor = "1/1.33")

        val expectedDiagonal = 16.0 / 1.33
        assertEquals(expectedDiagonal, metrics.diagonalMm, 1e-9)
        assertEquals(expectedDiagonal * 4.0 / 5.0, metrics.widthMm, 1e-9)
        assertEquals(expectedDiagonal * 3.0 / 5.0, metrics.heightMm, 1e-9)
        assertEquals(metrics.widthMm * metrics.heightMm, metrics.areaSqMm, 1e-9)
        assertEquals("1/1.33", metrics.sensorName)
        assertEquals(SensorSource.MANUAL, metrics.source)
    }

    @Test
    fun calculateNativeSensorMetrics_malformedManualDescriptorReturnsZeroWithFailureLabel() {
        val malformed = calculateNativeSensorMetrics(sensorSpec = null, manualDescriptor = "1/1.28/2")
        val blank = calculateNativeSensorMetrics(sensorSpec = null, manualDescriptor = "  ")

        assertEquals(0.0, malformed.areaSqMm, 0.0)
        assertEquals("1/1.28/2", malformed.sensorName)
        assertEquals(0.0, blank.areaSqMm, 0.0)
        assertEquals("N/A", blank.sensorName)
    }
}
