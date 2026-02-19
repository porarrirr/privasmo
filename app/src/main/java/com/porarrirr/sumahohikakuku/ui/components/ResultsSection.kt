package com.porarrirr.sumahohikakuku.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.data.GraphSettings
import com.porarrirr.sumahohikakuku.model.ComparisonResults
import kotlin.math.roundToInt

@Composable
internal fun ComparisonGraphSection(
    results: ComparisonResults,
    graphSettings: GraphSettings,
    isExporting: Boolean,
    graphRangeIndices: ClosedFloatingPointRange<Float>,
    onGraphRangeChanged: (ClosedFloatingPointRange<Float>) -> Unit,
    onShareResultsImage: () -> Unit
) {
    val focalLengths = results.focalLengths
    if (focalLengths.isEmpty()) return
    val sliderSteps = (focalLengths.size - 2).coerceAtLeast(0)
    val graphStartIndex = graphRangeIndices.start.roundToInt()
        .coerceIn(0, focalLengths.lastIndex)
    val graphEndIndex = graphRangeIndices.endInclusive.roundToInt()
        .coerceIn(graphStartIndex, focalLengths.lastIndex)
    val graphFocalRange = focalLengths[graphStartIndex]..focalLengths[graphEndIndex]

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.section_comparison_graph),
            style = MaterialTheme.typography.titleLarge
        )

        Button(
            onClick = onShareResultsImage,
            enabled = !isExporting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_share_results_image))
        }

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_graph_display_range),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(
                        R.string.label_graph_range_compact,
                        formatFocalLength(graphFocalRange.start),
                        formatFocalLength(graphFocalRange.endInclusive)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (focalLengths.size > 1) {
                    RangeSlider(
                        value = graphRangeIndices,
                        onValueChange = { range ->
                            val start = range.start.roundToInt()
                                .coerceIn(0, focalLengths.lastIndex)
                            val end = range.endInclusive.roundToInt()
                                .coerceIn(0, focalLengths.lastIndex)
                            val minIndex = start.coerceAtMost(end)
                            val maxIndex = end.coerceAtLeast(start)
                            onGraphRangeChanged(minIndex.toFloat()..maxIndex.toFloat())
                        },
                        valueRange = 0f..focalLengths.lastIndex.toFloat(),
                        steps = sliderSteps,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(
                            R.string.label_left_edge,
                            formatFocalLength(graphFocalRange.start)
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = stringResource(
                            R.string.label_right_edge,
                            formatFocalLength(graphFocalRange.endInclusive)
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        ChartsSection(
            results = results,
            graphSettings = graphSettings,
            focalRange = graphFocalRange
        )
    }
}

@Composable
internal fun SensorDetailsSection(
    results: ComparisonResults,
    selectedFocalLength: Double,
    onFocalLengthChanged: (Double) -> Unit
) {
    val focalLengths = results.focalLengths
    if (focalLengths.isEmpty()) return

    val selectedIndex = focalLengths.indexOf(selectedFocalLength)
        .takeIf { it >= 0 } ?: 0
    val sliderSteps = (focalLengths.size - 2).coerceAtLeast(0)
    val focalLength = focalLengths[selectedIndex]

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.label_selected_focal_length,
                        formatFocalLength(focalLength)
                    ),
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = selectedIndex.toFloat(),
                    onValueChange = {
                        val index = it.roundToInt().coerceIn(0, focalLengths.lastIndex)
                        onFocalLengthChanged(focalLengths[index])
                    },
                    valueRange = 0f..focalLengths.lastIndex.toFloat(),
                    steps = sliderSteps,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(results.devices) { device ->
                DeviceMetricsCard(
                    device = device,
                    focalLength = focalLength,
                    modifier = Modifier.widthIn(min = 260.dp, max = 360.dp)
                )
            }
        }
    }
}
