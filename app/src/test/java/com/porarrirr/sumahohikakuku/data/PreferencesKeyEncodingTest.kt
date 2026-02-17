package com.porarrirr.sumahohikakuku.data

import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.UUID
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PreferencesKeyEncodingTest {
    @Test
    fun encodePreferencesKeyComponent_createsKeySafeString() {
        val raw = "custom:${UUID.randomUUID()}"
        val encoded = encodePreferencesKeyComponent(raw)

        stringPreferencesKey("custom_sensor_$encoded")
    }

    @Test
    fun encodePreferencesKeyComponent_distinguishesColonAndUnderscore() {
        val colon = encodePreferencesKeyComponent("custom:1")
        val underscore = encodePreferencesKeyComponent("custom_1")

        assertNotEquals(colon, underscore)
    }
}

