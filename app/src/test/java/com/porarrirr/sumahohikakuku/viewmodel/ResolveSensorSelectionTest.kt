package com.porarrirr.sumahohikakuku.viewmodel

import com.porarrirr.sumahohikakuku.model.SensorSource
import com.porarrirr.sumahohikakuku.model.SensorSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveSensorSelectionTest {

    private val sensorSpec = SensorSpec(
        name = "Sony IMX999",
        value = "Sony IMX999",
        megapixels = 50.0,
        pixelSizeUm = 1.0,
        binningType = "None",
        manufacturer = "Sony",
        source = SensorSource.DATABASE
    )

    @Test
    fun resolveSensorSelection_returnsExistingSensorWhenValueMatches() {
        val lookup = mapOf(sensorSpec.value to sensorSpec)

        val resolved = resolveSensorSelection(
            rawValue = sensorSpec.value,
            manualDescriptor = "",
            sensorLookup = lookup
        )

        assertEquals(sensorSpec.value, resolved.value)
        assertNull(resolved.manualDescriptor)
    }

    @Test
    fun resolveSensorSelection_fallsBackToManualAndPreservesLegacyLabel() {
        val resolved = resolveSensorSelection(
            rawValue = "Legacy Sensor X",
            manualDescriptor = "",
            sensorLookup = emptyMap()
        )

        assertEquals("_MANUAL_INPUT_", resolved.value)
        assertEquals("Legacy Sensor X", resolved.manualDescriptor)
    }

    @Test
    fun resolveSensorSelection_manualSelectionUsesDefaultDescriptorWhenBlank() {
        val resolved = resolveSensorSelection(
            rawValue = "_MANUAL_INPUT_",
            manualDescriptor = "",
            sensorLookup = emptyMap()
        )

        assertEquals("_MANUAL_INPUT_", resolved.value)
        assertEquals("1/1.33", resolved.manualDescriptor)
    }

    @Test
    fun resolveSensorSelection_preservesProvidedManualDescriptor() {
        val resolved = resolveSensorSelection(
            rawValue = "_MANUAL_INPUT_",
            manualDescriptor = "1/1.12",
            sensorLookup = emptyMap()
        )

        assertEquals("_MANUAL_INPUT_", resolved.value)
        assertEquals("1/1.12", resolved.manualDescriptor)
    }
}
