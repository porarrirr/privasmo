package com.porarrirr.sumahohikakuku.model

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorCsvParsingTest {

    @Test
    fun parseSensorCsv_prependsManualOptionAndSortsSupportedManufacturers() {
        val raw = """
            # ignored comment
            Other Sensor,1200,1.0,Unknown
            Sony IMX989,5000,1.6,No
            OmniVision OV50H,5000,1.2,Yes
            Sony LYT-900,5000,1.6,Nona Bayer
            Samsung GN2 (S5KGN2),5000,1.4,16-IN-1
        """.trimIndent()

        val sensors = parseSensorCsv(raw)

        assertTrue(sensors.first().isManual)
        assertEquals(MANUAL_INPUT_SENSOR_VALUE, sensors.first().value)
        assertEquals(
            listOf(
                "Sony LYT-900",
                "Sony IMX989",
                "OmniVision OV50H",
                "Samsung GN2 (S5KGN2)",
                "Other Sensor"
            ),
            sensors.drop(1).map { it.name }
        )
    }

    @Test
    fun parseSensorCsv_handlesBomQuotedCommasEscapedQuotesAndInvalidRows() {
        val raw = "\uFEFF\"Sony \"\"IMX,999\"\"\",5000,1.0,No\n" +
            "Broken Sensor,not-number,1.0,No\n" +
            "Missing Column,1200,1.0"

        val parsed = parseSensorCsv(raw)
        val sensor = parsed.single { !it.isManual }

        assertEquals("Sony \"IMX,999\"", sensor.name)
        assertEquals(50.0, sensor.megapixels, 0.0)
        assertEquals("None", sensor.binningType)
    }

    @Test
    fun parseSensorCsv_interpretsMegapixelColumnAsHundredBasedValue() {
        val parsed = parseSensorCsv("Test Sensor,50,1.0,No")
        val sensor = parsed.single { !it.isManual }

        assertEquals(0.5, sensor.megapixels, 0.0)
    }

    @Test
    fun normalizeBinning_usesLocaleIndependentRules() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))

            assertEquals("Quad Bayer (2x2)", normalizeBinning("YES"))
            assertEquals("Nona (3x3)", normalizeBinning("Nona Bayer"))
            assertEquals("16-cell (4x4)", normalizeBinning("16-IN-1"))
            assertEquals("Unknown", normalizeBinning("Unknown"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun detectManufacturer_usesLocaleIndependentRules() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))

            assertEquals("OmniVision", detectManufacturer("OMNIVISION OV50"))
            assertEquals("SmartSens", detectManufacturer("SMARTSENS SC580"))
            assertEquals("Toshiba", detectManufacturer("TOSHIBA T4K37"))
            assertEquals("Other", detectManufacturer("Unknown Sensor"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun parseSensorCsv_skipsBlankCommentsAndMalformedRowsWithoutCreatingEmptySensors() {
        val parsed = parseSensorCsv(
            """

            # comment
            Good Sensor,1200,1.0,No
            Bad Pixel Size,1200,not-number,No
            Too,Many,Columns,In,Row
            """.trimIndent()
        )

        assertEquals(listOf("Good Sensor"), parsed.drop(1).map { it.name })
        assertFalse(parsed.drop(1).any { it.name.isBlank() })
    }
}
