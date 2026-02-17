package com.porarrirr.sumahohikakuku.domain.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputSanitizerTest {

    @Test
    fun sanitizeDecimalInput_normalizesFullWidthAndComma() {
        val sanitized = sanitizeDecimalInput("１，２３.４")
        assertEquals("1.234", sanitized)
    }

    @Test
    fun sanitizeHexInput_keepsOnlyHexCharsAndPrefix() {
        val sanitized = sanitizeHexInput("ab-cd#12zz")
        assertEquals("#ABCD12", sanitized)
    }

    @Test
    fun parseHexColor_returnsNormalizedHexWhenValid() {
        val parsed = parseHexColor("#a1b2c3")
        assertEquals("#A1B2C3", parsed)
    }

    @Test
    fun parseHexColor_returnsNullWhenInvalid() {
        assertNull(parseHexColor("#12G"))
    }
}
