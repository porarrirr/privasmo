package com.porarrirr.sumahohikakuku.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.model.FocalLengthMetrics
import com.porarrirr.sumahohikakuku.model.ProcessedDevice
import kotlin.math.max

@Composable
internal fun DeviceMetricsCard(
    device: ProcessedDevice,
    focalLength: Double,
    modifier: Modifier = Modifier
) {
    val metrics = device.metricsAt(focalLength) ?: return
    val fallbackColor = MaterialTheme.colorScheme.primary
    val color = remember(device.colorHex, fallbackColor) {
        runCatching { Color(AndroidColor.parseColor(device.colorHex)) }.getOrElse { fallbackColor }
    }
    var isDetailsExpanded by remember(device.name, device.colorHex) { mutableStateOf(false) }

    val sensorMetrics = metrics.baseLens.sensorMetrics
    val binning = sensorMetrics.binningType
    val nativePixelPitch = sensorMetrics.nativePixelSizeUm.takeIf { it > 0 }
        ?.let { "${it.format(2)} \u00B5m" }
    val binnedPixelPitch = computeBinnedPixelSize(sensorMetrics.nativePixelSizeUm, binning)
        ?.let { "${it.format(2)} \u00B5m" }
    val pixelPitchLabel = if (binnedPixelPitch != null) {
        stringResource(R.string.label_pixel_pitch_binned)
    } else {
        stringResource(R.string.label_pixel_pitch)
    }
    val pixelPitchValue = binnedPixelPitch ?: nativePixelPitch
        ?: stringResource(R.string.label_not_available)
    val opticalRangeValue = if (metrics.baseLens.isVariableOptical) {
        "${formatFocalLength(metrics.baseLens.nativeFocalLength35mm)}-${formatFocalLength(metrics.baseLens.opticalEndFocalLength35mm)} mm"
    } else {
        "${formatFocalLength(metrics.baseLens.nativeFocalLength35mm)} mm"
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = device.name,
                color = color,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            SensorVisualization(metrics = metrics, borderColor = color)

            DeviceMetricsHighlightGrid(
                items = listOf(
                    stringResource(R.string.metric_effective_area) to
                            "${metrics.effectiveAreaSqMm.format(2)} mm\u00B2",
                    pixelPitchLabel to pixelPitchValue,
                    stringResource(R.string.metric_total_light_intake) to
                            metrics.totalLightIntake.format(3)
                )
            )

            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            ) {
                DeviceMetricsKeyValueTable(
                    items = listOf(
                        stringResource(R.string.metric_effective_dimensions) to
                                "${metrics.effectiveWidthMm.format(2)} \u00D7 ${metrics.effectiveHeightMm.format(2)} mm",
                        stringResource(R.string.label_variable_optical_range) to opticalRangeValue,
                        stringResource(R.string.metric_actual_focal_length) to
                                "${metrics.opticalActualFocalLengthMm.format(2)} mm",
                        stringResource(R.string.label_estimated_f_number) to
                                "f/${metrics.effectiveFNumber.format(2)}",
                        stringResource(R.string.metric_effective_aperture) to
                                "${metrics.apertureDiameterMm.format(2)} mm",
                        stringResource(R.string.label_optical_zoom_ratio) to
                                "${metrics.opticalZoomRatio.format(2)}x",
                        stringResource(R.string.label_digital_crop_ratio) to
                                "${metrics.digitalCropRatio.format(2)}x",
                        stringResource(R.string.metric_sensor_used) to
                                sensorMetrics.sensorName,
                        stringResource(R.string.metric_lens_used) to stringResource(
                            R.string.value_lens_used_format,
                            metrics.baseLens.nativeFocalLength35mm.format(1)
                        )
                    ),
                    modifier = Modifier.padding(12.dp)
                )
            }

            val detailItems = buildList {
                add(stringResource(R.string.metric_binning_characteristic) to binning)
                nativePixelPitch?.let {
                    add(stringResource(R.string.metric_native_pixel_pitch) to it)
                }
                binnedPixelPitch?.let {
                    add(stringResource(R.string.metric_effective_pixel_pitch) to it)
                }
                add(
                    stringResource(R.string.metric_aperture_area) to
                            "${metrics.apertureAreaSqMm.format(2)} mm\u00B2"
                )
            }

            TextButton(
                onClick = { isDetailsExpanded = !isDetailsExpanded },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    if (isDetailsExpanded) {
                        stringResource(R.string.action_hide_details)
                    } else {
                        stringResource(R.string.action_show_details)
                    }
                )
            }

            if (isDetailsExpanded) {
                OutlinedCard(
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    DeviceMetricsKeyValueTable(
                        items = detailItems,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun DeviceMetricsHighlightGrid(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    val safeItems = items.filter { it.first.isNotBlank() && it.second.isNotBlank() }
    if (safeItems.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val rows = safeItems.chunked(2)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { (label, value) ->
                    MetricHighlightTile(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun MetricHighlightTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun DeviceMetricsKeyValueTable(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    val safeItems = items.filter { it.first.isNotBlank() && it.second.isNotBlank() }
    Column(modifier = modifier) {
        safeItems.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (index != safeItems.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
internal fun SensorVisualization(
    metrics: FocalLengthMetrics,
    borderColor: Color
) {
    val widthDp = max(metrics.effectiveWidthMm.toFloat() * SENSOR_VIZ_SCALE, 2f).dp
    val heightDp = max(metrics.effectiveHeightMm.toFloat() * SENSOR_VIZ_SCALE, 2f).dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(widthDp, heightDp)
                .border(2.dp, borderColor, RoundedCornerShape(4.dp))
                .background(Color.LightGray, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {}
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.label_dimensions_width_height,
                metrics.effectiveWidthMm,
                metrics.effectiveHeightMm
            ),
            fontSize = 12.sp
        )
    }
}
