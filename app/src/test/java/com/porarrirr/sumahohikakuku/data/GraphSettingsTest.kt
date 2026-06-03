package com.porarrirr.sumahohikakuku.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GraphSettingsTest {

    @Test
    fun glowLineWidth_scalesLineWidthByDefaultMultiplier() {
        val settings = GraphSettings(lineWidth = 3f)

        assertEquals(7.5f, settings.glowLineWidth, 0.0001f)
    }

    @Test
    fun exportAspectRatio_usesConfiguredWidthAndHeight() {
        val settings = GraphSettings(exportAspectWidth = 16, exportAspectHeight = 9)

        assertEquals(16f / 9f, settings.exportAspectRatio, 0.0001f)
    }
}
