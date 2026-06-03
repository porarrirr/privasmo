package com.porarrirr.sumahohikakuku.ui

import android.content.Context
import android.util.AttributeSet
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

@Suppress("UNUSED_PARAMETER")
class ChartMarkerView @JvmOverloads constructor(
    context: Context,
    _attrs: AttributeSet? = null,
    _defStyleAttr: Int = 0,
    private var yLabel: String = context.getString(R.string.chart_marker_default_label),
    private var yUnit: String? = null,
    private var yDecimals: Int = 2
) : MarkerView(context, R.layout.chart_marker_view) {

    constructor(
        context: Context,
        yLabel: String,
        yUnit: String?,
        yDecimals: Int
    ) : this(
        context = context,
        _attrs = null,
        _defStyleAttr = 0,
        yLabel = yLabel,
        yUnit = yUnit,
        yDecimals = yDecimals
    )

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
        xValueTextView.text = context.getString(R.string.chart_marker_focal_length_mm, focalLabel)

        val metrics = e.data as? FocalLengthMetrics
        if (metrics == null) {
            zoomInfoTextView.visibility = View.GONE
        } else {
            zoomInfoTextView.visibility = View.VISIBLE
            val baseNativeFocal = formatFocalLength(metrics.baseLens.nativeFocalLength35mm)
            zoomInfoTextView.text = if (metrics.baseLens.isVariableOptical || metrics.digitalCropRatio > 1.0001) {
                val opticalFocalLabel = formatFocalLength(metrics.opticalFocalLength35mm)
                val opticalZoomLabel = String.format(Locale.US, "%.2f", metrics.opticalZoomRatio)
                val cropLabel = String.format(Locale.US, "%.2f", metrics.digitalCropRatio)
                context.getString(
                    R.string.chart_marker_zoom_optical_crop,
                    opticalFocalLabel,
                    opticalZoomLabel,
                    cropLabel
                )
            } else {
                context.getString(R.string.chart_marker_zoom_optical, baseNativeFocal)
            }
        }

        val formattedY = String.format(Locale.US, "%.${yDecimals}f", e.y)
        val unitSuffix = yUnit?.takeIf { it.isNotBlank() }
            ?.let { context.getString(R.string.chart_marker_unit_prefix, it) }
            .orEmpty()
        yValueTextView.text = context.getString(R.string.chart_marker_value_format, yLabel, formattedY, unitSuffix)

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
