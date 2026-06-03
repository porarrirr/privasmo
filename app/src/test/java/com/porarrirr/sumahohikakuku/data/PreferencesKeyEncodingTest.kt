package com.porarrirr.sumahohikakuku.data

import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferencesKeyEncodingTest {

    @Test
    fun encodePreferencesKeyComponent_returnsStableLowercaseUtf8Hex() {
        val encoded = encodePreferencesKeyComponent("sensor:wide_1")

        assertEquals("73656e736f723a776964655f31", encoded)
    }

    @Test
    fun encodePreferencesKeyComponent_distinguishesReservedCharacters() {
        val colon = encodePreferencesKeyComponent("custom:1")
        val underscore = encodePreferencesKeyComponent("custom_1")

        assertNotEquals(colon, underscore)
    }

    @Test
    fun encodePreferencesKeyComponent_producesDataStoreSafeKeyComponents() {
        val encoded = encodePreferencesKeyComponent("custom:Sony IMX989/1")

        assertTrue(encoded.all { it in '0'..'9' || it in 'a'..'f' })
        stringPreferencesKey("custom_sensor_$encoded")
    }
}
