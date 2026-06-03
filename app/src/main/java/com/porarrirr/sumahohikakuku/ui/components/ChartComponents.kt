package com.porarrirr.sumahohikakuku.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.data.GraphSettings
import com.porarrirr.sumahohikakuku.model.ComparisonResults
import com.porarrirr.sumahohikakuku.model.FocalLengthMetrics
import java.util.Locale

@Composable
internal fun ChartsSection(
    results: ComparisonResults,
    graphSettings: GraphSettings,
    focalRange: ClosedFloatingPointRange<Double>? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ChartCard(
            title = stringResource(R.string.chart_title_effective_area),
            results = results,
            graphSettings = graphSettings,
            focalRange = focalRange,
            yLabel = stringResource(R.string.chart_y_label_effective_area),
            yUnit = "mm\u00B2",
            yDecimals = 2,
            dataExtractor = { metric -> metric.effectiveAreaSqMm.toFloat() }
        )
        ChartCard(
            title = stringResource(R.string.chart_title_light_intake),
            results = results,
            graphSettings = graphSettings,
            focalRange = focalRange,
            yLabel = stringResource(R.string.chart_y_label_light_intake),
            yDecimals = 3,
            dataExtractor = { metric -> metric.totalLightIntake.toFloat() }
        )
    }
}

@Composable
internal fun ChartCard(
    title: String,
    results: ComparisonResults,
    graphSettings: GraphSettings,
    focalRange: ClosedFloatingPointRange<Double>? = null,
    yLabel: String,
    yUnit: String? = null,
    yDecimals: Int = 2,
    dataExtractor: (FocalLengthMetrics) -> Float
) {
    val isDarkTheme = isSystemInDarkTheme()
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (isDarkTheme) 0.2f else 0.08f
    ).toArgb()
    val chartSurfaceColor = MaterialTheme.colorScheme.surface.toArgb()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    LineChart(context).apply {
                        description.isEnabled = false
                        axisRight.isEnabled = false
                        setDrawGridBackground(false)
                        setDrawBorders(false)
                        setTouchEnabled(true)
                        setPinchZoom(true)
                        setHighlightPerTapEnabled(false)
                        setHighlightPerDragEnabled(false)
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.granularity = 1f
                        xAxis.setDrawGridLines(false)
                        xAxis.setDrawAxisLine(false)
                        xAxis.valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                return formatFocalLength(value.toDouble())
                            }
                        }
                        axisLeft.setDrawGridLines(true)
                        axisLeft.setLabelCount(4, false)
                        axisLeft.setDrawAxisLine(false)
                        axisLeft.axisMinimum = 0f
                        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                        legend.orientation = Legend.LegendOrientation.HORIZONTAL
                        legend.setDrawInside(false)
                    }
                },
                update = { chart ->
                    val fullMinFocal = results.focalLengths.firstOrNull() ?: 0.0
                    val fullMaxFocal = results.focalLengths.lastOrNull() ?: fullMinFocal
                    val rawMinFocal = focalRange?.start ?: fullMinFocal
                    val rawMaxFocal = focalRange?.endInclusive ?: fullMaxFocal
                    val clampedMinFocal = rawMinFocal
                        .coerceAtLeast(fullMinFocal)
                        .coerceAtMost(fullMaxFocal)
                    val clampedMaxFocal = rawMaxFocal
                        .coerceAtLeast(clampedMinFocal)
                        .coerceAtMost(fullMaxFocal)
                    val visibleFocals = results.focalLengths.filter { focal ->
                        focal in clampedMinFocal..clampedMaxFocal
                    }

                    chart.xAxis.textColor = axisTextColor
                    chart.xAxis.textSize = 12f
                    chart.xAxis.axisMinimum = clampedMinFocal.toFloat()
                    chart.xAxis.axisMaximum = clampedMaxFocal.toFloat()
                    chart.axisLeft.textColor = axisTextColor
                    chart.axisLeft.textSize = 12f
                    chart.axisLeft.gridColor = gridColor
                    chart.legend.textColor = axisTextColor
                    chart.legend.textSize = 12f
                    chart.legend.form = Legend.LegendForm.CIRCLE
                    chart.legend.formSize = 10f

                    val axisLabelDecimals = yDecimals.coerceAtMost(2)
                    chart.axisLeft.valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val formatted = String.format(
                                Locale.US, "%.${axisLabelDecimals}f", value
                            )
                            val unitSuffix = yUnit?.takeIf { it.isNotBlank() }
                                ?.let { " $it" }.orEmpty()
                            return "$formatted$unitSuffix"
                        }
                    }

                    val fillTopAlpha = when (results.devices.size) {
                        1 -> 80
                        2 -> 64
                        else -> 52
                    }
                    val glowAlpha = if (isDarkTheme) 64 else 40

                    val dataSets = mutableListOf<ILineDataSet>()
                    val legendEntries = mutableListOf<LegendEntry>()
                    results.devices.forEach { device ->
                        val parsedColor = runCatching { AndroidColor.parseColor(device.colorHex) }
                            .getOrDefault(AndroidColor.DKGRAY)
                        val nativeFocals = device.lenses
                            .flatMap { lens ->
                                listOf(lens.nativeFocalLength35mm, lens.opticalEndFocalLength35mm)
                            }
                            .distinct()

                        val markerDrawable = createLensMarkerDrawable(
                            context = chart.context,
                            accentColor = parsedColor,
                            surfaceColor = chartSurfaceColor
                        )

                        val entries = visibleFocals.mapNotNull { focal ->
                            val metrics = device.metricsAt(focal) ?: return@mapNotNull null
                            Entry(focal.toFloat(), dataExtractor(metrics)).apply {
                                data = metrics
                                if (isApproximatelyIn(focal, nativeFocals)) {
                                    icon = markerDrawable
                                }
                            }
                        }

                        dataSets += LineDataSet(entries, "").apply {
                            color = withAlpha(parsedColor, glowAlpha)
                            setDrawValues(false)
                            setDrawCircles(false)
                            setDrawFilled(false)
                            setDrawIcons(false)
                            lineWidth = graphSettings.glowLineWidth
                            mode = LineDataSet.Mode.LINEAR
                            isHighlightEnabled = false
                        }

                        dataSets += LineDataSet(entries, device.name).apply {
                            color = parsedColor
                            highLightColor = parsedColor
                            setDrawValues(false)
                            setDrawCircles(false)
                            setDrawFilled(true)
                            setDrawIcons(true)
                            lineWidth = graphSettings.lineWidth
                            mode = LineDataSet.Mode.LINEAR
                            fillDrawable = createLineFillDrawable(
                                lineColor = parsedColor,
                                topAlpha = fillTopAlpha
                            )
                        }

                        legendEntries += LegendEntry().apply {
                            label = device.name
                            formColor = parsedColor
                            form = Legend.LegendForm.CIRCLE
                        }
                    }

                    chart.legend.setCustom(legendEntries)
                    chart.data = LineData(dataSets)
                    chart.axisLeft.setDrawLabels(true)
                    chart.axisLeft.axisMinimum = 0f
                    chart.invalidate()
                }
            )
        }
    }
}
