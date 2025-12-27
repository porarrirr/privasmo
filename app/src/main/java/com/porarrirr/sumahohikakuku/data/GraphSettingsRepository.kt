package com.porarrirr.sumahohikakuku.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class GraphSettings(
    val lineWidth: Float = DEFAULT_LINE_WIDTH
) {
    val glowLineWidth: Float get() = lineWidth * DEFAULT_GLOW_MULTIPLIER

    companion object {
        const val MIN_LINE_WIDTH = 1f
        const val MAX_LINE_WIDTH = 8f
        const val DEFAULT_LINE_WIDTH = 4f
        const val DEFAULT_GLOW_MULTIPLIER = 2.5f
    }
}

private val Context.graphSettingsDataStore by preferencesDataStore(name = "graph_settings")

private val LINE_WIDTH_KEY = floatPreferencesKey("chart_line_width")

class GraphSettingsRepository(
    private val context: Context
) {

    val settingsFlow: Flow<GraphSettings> = context.graphSettingsDataStore.data.map { preferences ->
        val raw = preferences[LINE_WIDTH_KEY] ?: GraphSettings.DEFAULT_LINE_WIDTH
        val width = raw
            .takeIf { it.isFinite() }
            ?.coerceIn(GraphSettings.MIN_LINE_WIDTH, GraphSettings.MAX_LINE_WIDTH)
            ?: GraphSettings.DEFAULT_LINE_WIDTH
        GraphSettings(lineWidth = width)
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

    suspend fun reset() {
        context.graphSettingsDataStore.edit { preferences ->
            preferences.remove(LINE_WIDTH_KEY)
        }
    }
}

