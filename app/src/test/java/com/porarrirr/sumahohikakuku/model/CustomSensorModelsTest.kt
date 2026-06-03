package com.porarrirr.sumahohikakuku.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CustomSensorModelsTest {

    @Test
    fun toSensorSpec_preservesIdAsValueAndNormalizesDerivedFields() {
        val entry = CustomSensorEntry(
            id = "custom:sony-lyt900",
            name = "Sony LYT-900",
            megapixels = 50.0,
            pixelSizeUm = 1.6,
            binningType = "16-IN-1"
        )

        val spec = entry.toSensorSpec()

        assertEquals("custom:sony-lyt900", spec.value)
        assertEquals("Sony", spec.manufacturer)
        assertEquals("16-cell (4x4)", spec.binningType)
        assertEquals(SensorSource.DATABASE, spec.source)
        assertFalse(spec.isManual)
    }
}
