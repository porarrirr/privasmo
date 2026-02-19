package com.porarrirr.sumahohikakuku.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.data.GraphSettings
import com.porarrirr.sumahohikakuku.model.ComparisonResults
import com.porarrirr.sumahohikakuku.model.FocalLengthMetrics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal enum class ExportChartType(
    val fileSuffix: String,
    @StringRes val titleRes: Int,
    @StringRes val yLabelRes: Int,
    val yUnit: String?,
    val yDecimals: Int,
    val dataExtractor: (FocalLengthMetrics) -> Float
) {
    EFFECTIVE_AREA(
        fileSuffix = "effective_area",
        titleRes = R.string.chart_title_effective_area,
        yLabelRes = R.string.chart_y_label_effective_area,
        yUnit = "mm\u00B2",
        yDecimals = 2,
        dataExtractor = { metric -> metric.effectiveAreaSqMm.toFloat() }
    ),
    LIGHT_INTAKE(
        fileSuffix = "light_intake",
        titleRes = R.string.chart_title_light_intake,
        yLabelRes = R.string.chart_y_label_light_intake,
        yUnit = null,
        yDecimals = 3,
        dataExtractor = { metric -> metric.totalLightIntake.toFloat() }
    )
}

@Composable
internal fun ExportChartImageContent(
    results: ComparisonResults,
    graphSettings: GraphSettings,
    chartType: ExportChartType,
    focalRange: ClosedFloatingPointRange<Double>,
    exportedAtEpochMillis: Long
) {
    val exportedAtLabel = remember(exportedAtEpochMillis) {
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            .format(Date(exportedAtEpochMillis))
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.export_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.export_label_created_at, exportedAtLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.export_label_range,
                    formatFocalLength(focalRange.start),
                    formatFocalLength(focalRange.endInclusive)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ChartCard(
                title = stringResource(chartType.titleRes),
                results = results,
                graphSettings = graphSettings,
                focalRange = focalRange,
                yLabel = stringResource(chartType.yLabelRes),
                yUnit = chartType.yUnit,
                yDecimals = chartType.yDecimals,
                dataExtractor = chartType.dataExtractor
            )
        }
    }
}
