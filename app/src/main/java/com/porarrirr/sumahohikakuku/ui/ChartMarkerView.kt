package com.porarrirr.sumahohikakuku.ui

import android.content.Context
import android.view.View
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.model.FocalLengthMetrics
import java.util.Locale
import kotlin.math.roundToInt

class ChartMarkerView(
    context: Context,
    private val yLabel: String,
    private val yUnit: String?,
    private val yDecimals: Int
) : MarkerView(context, R.layout.chart_marker_view) {

    private val titleTextView: TextView = findViewById(R.id.marker_title)
    private val xValueTextView: TextView = findViewById(R.id.marker_x_value)
    private val zoomInfoTextView: TextView = findViewById(R.id.marker_zoom_info)
    private val yValueTextView: TextView = findViewById(R.id.marker_y_value)

    override fun refreshContent(e: Entry, highlight: Highlight) {
        val dataSetLabel = chartView?.data?.getDataSetByIndex(highlight.dataSetIndex)?.label
        if (dataSetLabel.isNullOrBlank()) {
            titleTextView.visibility = View.GONE
        } else {
            titleTextView.visibility = View.VISIBLE
            titleTextView.text = dataSetLabel
        }

        val focalLabel = formatFocalLength(e.x.toDouble())
        xValueTextView.text = "焦点距離: ${focalLabel}mm"

        val metrics = e.data as? FocalLengthMetrics
        if (metrics == null) {
            zoomInfoTextView.visibility = View.GONE
        } else {
            zoomInfoTextView.visibility = View.VISIBLE
            val baseNativeFocal = formatFocalLength(metrics.baseLens.nativeFocalLength35mm)
            zoomInfoTextView.text = if (metrics.zoomRatio > 1.0001) {
                val zoomLabel = String.format(Locale.US, "%.2f", metrics.zoomRatio)
                "デジタルズーム (クロップ): ${zoomLabel}x (元: ${baseNativeFocal}mm)"
            } else {
                "光学 (ネイティブレンズ: ${baseNativeFocal}mm)"
            }
        }

        val formattedY = String.format(Locale.US, "%.${yDecimals}f", e.y)
        val unitSuffix = yUnit?.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
        yValueTextView.text = "$yLabel: $formattedY$unitSuffix"

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}

private const val FOCAL_EPSILON = 0.01

private fun formatFocalLength(value: Double): String {
    val rounded = value.roundToInt().toDouble()
    return if (kotlin.math.abs(value - rounded) < FOCAL_EPSILON) {
        rounded.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}
