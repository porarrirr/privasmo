package com.porarrirr.sumahohikakuku.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class GraphSettings(
    val lineWidth: Float = DEFAULT_LINE_WIDTH,
    val exportAspectWidth: Int = DEFAULT_EXPORT_ASPECT_WIDTH,
    val exportAspectHeight: Int = DEFAULT_EXPORT_ASPECT_HEIGHT
) {
    val glowLineWidth: Float get() = lineWidth * DEFAULT_GLOW_MULTIPLIER
    val exportAspectRatio: Float get() = exportAspectWidth.toFloat() / exportAspectHeight.toFloat()

    companion object {
        const val MIN_LINE_WIDTH = 1f
        const val MAX_LINE_WIDTH = 8f
        const val DEFAULT_LINE_WIDTH = 4f
        const val DEFAULT_GLOW_MULTIPLIER = 2.5f
        const val MIN_EXPORT_ASPECT_COMPONENT = 1
        const val MAX_EXPORT_ASPECT_COMPONENT = 100
        const val DEFAULT_EXPORT_ASPECT_WIDTH = 4
        const val DEFAULT_EXPORT_ASPECT_HEIGHT = 3
    }
}

private val Context.graphSettingsDataStore by preferencesDataStore(name = "graph_settings")

private val LINE_WIDTH_KEY = floatPreferencesKey("chart_line_width")
private val EXPORT_ASPECT_WIDTH_KEY = intPreferencesKey("export_aspect_width")
private val EXPORT_ASPECT_HEIGHT_KEY = intPreferencesKey("export_aspect_height")

class GraphSettingsRepository(
    private val context: Context
) {

    val settingsFlow: Flow<GraphSettings> = context.graphSettingsDataStore.data.map { preferences ->
        val raw = preferences[LINE_WIDTH_KEY] ?: GraphSettings.DEFAULT_LINE_WIDTH
        val width = raw
            .takeIf { it.isFinite() }
            ?.coerceIn(GraphSettings.MIN_LINE_WIDTH, GraphSettings.MAX_LINE_WIDTH)
            ?: GraphSettings.DEFAULT_LINE_WIDTH
        val aspectWidth = normalizeAspectComponent(
            value = preferences[EXPORT_ASPECT_WIDTH_KEY],
            fallback = GraphSettings.DEFAULT_EXPORT_ASPECT_WIDTH
        )
        val aspectHeight = normalizeAspectComponent(
            value = preferences[EXPORT_ASPECT_HEIGHT_KEY],
            fallback = GraphSettings.DEFAULT_EXPORT_ASPECT_HEIGHT
        )
        GraphSettings(
            lineWidth = width,
            exportAspectWidth = aspectWidth,
            exportAspectHeight = aspectHeight
        )
    }

    suspend fun setLineWidth(value: Float) {
        val normalized = value
            .takeIf { it.isFinite() }
            ?.coerceIn(GraphSettings.MIN_LINE_WIDTH, GraphSettings.MAX_LINE_WIDTH)
            ?: GraphSettings.DEFAULT_LINE_WIDTH
        context.graphSettingsDataStore.edit { preferences ->
            preferences[LINE_WIDTH_KEY] = normalized
        }
    }

    suspend fun setExportAspectRatio(width: Int, height: Int) {
        val normalizedWidth = normalizeAspectComponent(width, GraphSettings.DEFAULT_EXPORT_ASPECT_WIDTH)
        val normalizedHeight = normalizeAspectComponent(height, GraphSettings.DEFAULT_EXPORT_ASPECT_HEIGHT)
        context.graphSettingsDataStore.edit { preferences ->
            preferences[EXPORT_ASPECT_WIDTH_KEY] = normalizedWidth
            preferences[EXPORT_ASPECT_HEIGHT_KEY] = normalizedHeight
        }
    }

    suspend fun reset() {
        context.graphSettingsDataStore.edit { preferences ->
            preferences.remove(LINE_WIDTH_KEY)
            preferences.remove(EXPORT_ASPECT_WIDTH_KEY)
            preferences.remove(EXPORT_ASPECT_HEIGHT_KEY)
        }
    }

    private fun normalizeAspectComponent(value: Int?, fallback: Int): Int {
        return (value ?: fallback).coerceIn(
            GraphSettings.MIN_EXPORT_ASPECT_COMPONENT,
            GraphSettings.MAX_EXPORT_ASPECT_COMPONENT
        )
    }
}

