package com.porarrirr.sumahohikakuku.domain.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputSanitizerTest {

    @Test
    fun sanitizeDecimalInput_keepsDigitsAndOnlyTheFirstDecimalSeparator() {
        val sanitized = sanitizeDecimalInput(" １２,３．４abc5 ")

        assertEquals("12.345", sanitized)
    }

    @Test
    fun sanitizeDecimalInput_returnsEmptyWhenInputHasNoDecimalCharacters() {
        val sanitized = sanitizeDecimalInput("abc-+")

        assertEquals("", sanitized)
    }

    @Test
    fun sanitizeHexInput_normalizesCasePrefixAndLength() {
        val sanitized = sanitizeHexInput("ab-cd#12zz34")

        assertEquals("#ABCD12", sanitized)
    }

    @Test
    fun sanitizeHexInput_returnsPrefixWhenNoHexDigitsRemain() {
        assertEquals("#", sanitizeHexInput(""))
        assertEquals("#", sanitizeHexInput("zzzz"))
    }

    @Test
    fun parseHexColor_acceptsPrefixedAndUnprefixedColors() {
        assertEquals("#A1B2C3", parseHexColor("#a1b2c3"))
        assertEquals("#A1B2C3", parseHexColor("a1b2c3"))
    }

    @Test
    fun parseHexColor_rejectsInvalidLengthOrCharacters() {
        assertNull(parseHexColor("#12G456"))
        assertNull(parseHexColor("#12345"))
        assertNull(parseHexColor("#1234567"))
    }
}
