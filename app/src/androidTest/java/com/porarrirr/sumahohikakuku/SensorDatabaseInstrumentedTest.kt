package com.porarrirr.sumahohikakuku

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.porarrirr.sumahohikakuku.model.parseSensorCsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SensorDatabaseInstrumentedTest {

    @Test
    fun appContext_usesReleaseApplicationId() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals("com.porarrirr.sumahohikakuku", context.packageName)
    }

    @Test
    fun rawSensorDatabase_parsesManualEntryAndKnownSensors() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val raw = context.resources
            .openRawResource(R.raw.sensor_database)
            .bufferedReader()
            .use { it.readText() }

        val sensors = parseSensorCsv(raw)

        assertTrue(sensors.first().isManual)
        assertTrue(sensors.any { it.name == "Sony IMX989" })
        assertTrue(sensors.any { it.name == "Sony LYT-900 (IMX06A)" })
        assertFalse(sensors.drop(1).any { it.name.isBlank() })
    }
}
