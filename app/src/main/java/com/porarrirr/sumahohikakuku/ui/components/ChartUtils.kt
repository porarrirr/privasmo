package com.porarrirr.sumahohikakuku.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import androidx.core.graphics.drawable.DrawableCompat
import com.porarrirr.sumahohikakuku.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val SENSOR_VIZ_SCALE = 6f
internal const val FOCAL_EPSILON = 0.01

internal val DEVICE_COLOR_PALETTE = listOf(
    "#2563EB", "#1D4ED8", "#1E40AF",
    "#7C3AED", "#9333EA", "#A855F7",
    "#6366F1", "#818CF8", "#A5B4FC",
    "#0EA5E9", "#22D3EE", "#38BDF8",
    "#14B8A6", "#10B981", "#34D399",
    "#059669", "#22C55E", "#4ADE80",
    "#DC2626", "#F97316", "#F59E0B",
    "#FB923C", "#EC4899", "#F472B6",
    "#FB7185"
)

internal fun formatFocalLength(value: Double): String {
    val rounded = value.roundToInt().toDouble()
    return if (abs(value - rounded) < FOCAL_EPSILON) {
        rounded.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

internal fun isApproximatelyIn(value: Double, candidates: List<Double>): Boolean {
    return candidates.any { candidate -> abs(candidate - value) < FOCAL_EPSILON }
}

internal fun computeBinnedPixelSize(nativePixelSize: Double, binningType: String): Double? {
    if (nativePixelSize <= 0.0) return null
    val lower = binningType.lowercase(Locale.US)
    val factor = when {
        "quad" in lower || "2x2" in lower -> 2.0
        "nona" in lower || "3x3" in lower -> 3.0
        "16" in lower || "4x4" in lower -> 4.0
        else -> return null
    }
    return nativePixelSize * factor
}

internal fun Double.format(decimals: Int): String {
    return String.format(Locale.US, "%.${decimals}f", this)
}

internal fun withAlpha(color: Int, alpha: Int): Int {
    return AndroidColor.argb(
        alpha.coerceIn(0, 255),
        AndroidColor.red(color),
        AndroidColor.green(color),
        AndroidColor.blue(color)
    )
}

internal fun createLineFillDrawable(lineColor: Int, topAlpha: Int): GradientDrawable {
    return GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(withAlpha(lineColor, topAlpha), withAlpha(lineColor, 0))
    )
}

internal fun createLensMarkerDrawable(
    context: android.content.Context,
    accentColor: Int,
    surfaceColor: Int
): LayerDrawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (density * 16f).roundToInt().coerceAtLeast(1)
    val strokePx = (density * 2f).roundToInt().coerceAtLeast(1)
    val insetPx = (density * 4f).roundToInt().coerceAtLeast(1)

    val background = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(surfaceColor)
        setStroke(strokePx, accentColor)
    }

    val lens = AppCompatResources.getDrawable(
        context,
        R.drawable.ic_lens_marker
    )?.mutate()?.also { DrawableCompat.setTint(it, accentColor) }

    return LayerDrawable(arrayOf(background, lens ?: background)).apply {
        setLayerInset(1, insetPx, insetPx, insetPx, insetPx)
        setBounds(0, 0, sizePx, sizePx)
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.bringIntoViewOnFocus(): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    bringIntoViewRequester(requester)
        .onFocusEvent { focusState ->
            if (focusState.isFocused) {
                scope.launch {
                    delay(150)
                    requester.bringIntoView()
                }
            }
        }
}

@Composable
internal fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(indication = null, interactionSource = interactionSource) { onClick() }
}
