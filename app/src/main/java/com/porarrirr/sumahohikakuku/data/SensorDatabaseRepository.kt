package com.porarrirr.sumahohikakuku.data

import android.content.Context
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.model.SensorSpec
import com.porarrirr.sumahohikakuku.model.parseSensorCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SensorDatabaseRepository(
    private val context: Context
) {
    suspend fun loadSensors(): Result<List<SensorSpec>> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.resources
                .openRawResource(R.raw.sensor_database)
                .bufferedReader()
                .use { it.readText() }
            parseSensorCsv(raw)
        }
    }
}
