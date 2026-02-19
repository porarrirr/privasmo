package com.porarrirr.sumahohikakuku.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.domain.input.parseHexColor
import com.porarrirr.sumahohikakuku.domain.input.sanitizeHexInput
import java.util.Locale

/**
 * Collapsible color selector. Collapsed state shows only a color dot + label;
 * expanded state reveals swatch grid, palette button, and hex input.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorSelector(
    selectedColor: String,
    availableColors: List<String>,
    onSelectColor: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isPaletteDialogOpen by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Collapsed header - always visible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val previewColor = runCatching { Color(AndroidColor.parseColor(selectedColor)) }
                .getOrElse { MaterialTheme.colorScheme.primary }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(previewColor, RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                    .semantics { contentDescription = selectedColor }
            )
            Text(
                text = stringResource(R.string.label_color),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Expanded content
        AnimatedVisibility(visible = isExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 6
                ) {
                    availableColors.forEach { colorHex ->
                        val color = Color(AndroidColor.parseColor(colorHex))
                        val borderColor = if (colorHex.equals(selectedColor, ignoreCase = true)) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color = color, shape = RoundedCornerShape(6.dp))
                                .border(
                                    BorderStroke(2.dp, borderColor),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(2.dp)
                                .background(color.copy(alpha = 0.9f), shape = RoundedCornerShape(4.dp))
                                .padding(1.dp)
                                .clickableWithoutRipple {
                                    onSelectColor(colorHex.uppercase(Locale.US))
                                }
                                .semantics { contentDescription = colorHex }
                        )
                    }
                }

                TextButton(onClick = { isPaletteDialogOpen = true }) {
                    Text(stringResource(R.string.action_open_color_palette))
                }

                var customColorInput by remember { mutableStateOf(selectedColor.uppercase(Locale.US)) }
                LaunchedEffect(selectedColor) {
                    customColorInput = selectedColor.uppercase(Locale.US)
                }
                val isValidHex = customColorInput.length == 7 && parseHexColor(customColorInput) != null

                OutlinedTextField(
                    value = customColorInput,
                    onValueChange = { newValue ->
                        val normalized = sanitizeHexInput(newValue)
                        customColorInput = normalized
                        parseHexColor(normalized)?.let(onSelectColor)
                    },
                    label = { Text(stringResource(R.string.label_custom_color)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus(),
                    isError = !isValidHex,
                    supportingText = {
                        if (!isValidHex) {
                            Text(stringResource(R.string.helper_hex_six_digits))
                        }
                    },
                    trailingIcon = {
                        val trailingPreviewColor = runCatching {
                            Color(AndroidColor.parseColor(customColorInput))
                        }.getOrNull()
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = trailingPreviewColor ?: Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    },
                    singleLine = true
                )
            }
        }
    }

    if (isPaletteDialogOpen) {
        ColorPaletteDialog(
            selectedColor = selectedColor,
            onSelectColor = onSelectColor,
            onDismiss = { isPaletteDialogOpen = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorPaletteDialog(
    selectedColor: String,
    onSelectColor: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_color_palette)) },
        text = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 6
            ) {
                DEVICE_COLOR_PALETTE.forEach { colorHex ->
                    val color = Color(AndroidColor.parseColor(colorHex))
                    val isSelected = colorHex.equals(selectedColor, ignoreCase = true)
                    val border = if (isSelected) {
                        BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(color = color, shape = RoundedCornerShape(8.dp))
                            .border(border, shape = RoundedCornerShape(8.dp))
                            .clickableWithoutRipple {
                                onSelectColor(colorHex.uppercase(Locale.US))
                                onDismiss()
                            }
                            .semantics { contentDescription = colorHex }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}
