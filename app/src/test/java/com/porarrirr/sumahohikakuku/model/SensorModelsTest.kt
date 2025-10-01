package com.porarrirr.sumahohikakuku.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorModelsTest {

    @Test
    fun isValidManualSensorDescriptor_acceptsCommaDecimal() {
        assertTrue(isValidManualSensorDescriptor("1/1,33"))
    }

    @Test
    fun calculateNativeSensorMetrics_handlesCommaDecimal() {
        val metrics = calculateNativeSensorMetrics(sensorSpec = null, manualDescriptor = "1/1,33")

        assertTrue(metrics.areaSqMm > 0.0)
        assertEquals("1/1,33", metrics.sensorName)
    }
}
