package com.porarrirr.sumahohikakuku.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.domain.input.sanitizeDecimalInput
import com.porarrirr.sumahohikakuku.model.MANUAL_INPUT_SENSOR_VALUE
import com.porarrirr.sumahohikakuku.model.SensorSpec
import com.porarrirr.sumahohikakuku.model.isValidManualSensorDescriptor
import com.porarrirr.sumahohikakuku.ui.SensorComparisonActions
import com.porarrirr.sumahohikakuku.viewmodel.LensInputState

@Composable
internal fun LensCard(
    deviceId: Long,
    lens: LensInputState,
    availableSensors: List<SensorSpec>,
    actions: SensorComparisonActions,
    canRemove: Boolean = true,
    modifier: Modifier = Modifier
) {
    val selectedSensor = availableSensors.firstOrNull { it.value == lens.selectedSensorValue }
    val sensorLabel = when {
        selectedSensor == null -> stringResource(R.string.label_manual_input)
        selectedSensor.isManual -> stringResource(R.string.label_manual_input)
        else -> selectedSensor.name
    }
    var isSensorPickerOpen by remember { mutableStateOf(false) }
    val removeIconTint = if (canRemove) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
    }

    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                CompactDecimalField(
                    value = lens.nativeFocalLength,
                    onValueChange = {
                        actions.updateLensFocalLength(deviceId, lens.id, sanitizeDecimalInput(it))
                    },
                    label = stringResource(R.string.label_focal_length),
                    placeholder = "24",
                    width = 82.dp
                )
                CompactDecimalField(
                    value = lens.fNumber,
                    onValueChange = {
                        actions.updateLensFNumber(deviceId, lens.id, sanitizeDecimalInput(it))
                    },
                    label = stringResource(R.string.label_f_number),
                    placeholder = "1.8",
                    width = 66.dp
                )
                SensorSelectorButton(
                    label = sensorLabel,
                    onClick = { isSensorPickerOpen = true },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { actions.removeLens(deviceId, lens.id) },
                    enabled = canRemove
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = stringResource(R.string.content_desc_delete_lens),
                        tint = removeIconTint
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CompactDecimalField(
                    value = lens.opticalEndFocalLength,
                    onValueChange = {
                        actions.updateLensOpticalEndFocalLength(deviceId, lens.id, sanitizeDecimalInput(it))
                    },
                    label = stringResource(R.string.label_optical_end_focal_length),
                    placeholder = "100",
                    width = 112.dp
                )
                CompactDecimalField(
                    value = lens.endFNumber,
                    onValueChange = {
                        actions.updateLensEndFNumber(deviceId, lens.id, sanitizeDecimalInput(it))
                    },
                    label = stringResource(R.string.label_end_f_number),
                    placeholder = "2.96",
                    width = 84.dp
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            if (isSensorPickerOpen) {
                SensorPickerSheet(
                    availableSensors = availableSensors,
                    selectedSensorValue = lens.selectedSensorValue,
                    onSelectSensor = { value ->
                        actions.updateLensSensorSelection(deviceId, lens.id, value)
                        isSensorPickerOpen = false
                    },
                    onClose = { isSensorPickerOpen = false }
                )
            }

            if (lens.selectedSensorValue == MANUAL_INPUT_SENSOR_VALUE) {
                val manualValid = isValidManualSensorDescriptor(lens.manualSensorDescriptor)
                CompactInputField(
                    value = lens.manualSensorDescriptor,
                    onValueChange = { actions.updateLensManualDescriptor(deviceId, lens.id, it) },
                    label = stringResource(R.string.label_manual_input_example),
                    placeholder = "1/1.33",
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus(),
                    keyboardType = KeyboardType.Text,
                    isError = !manualValid
                )
                if (!manualValid) {
                    Text(
                        text = stringResource(R.string.helper_invalid_fraction_format),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactDecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    width: Dp
) {
    CompactInputField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        modifier = Modifier.width(width),
        keyboardType = KeyboardType.Decimal
    )
}

@Composable
private fun CompactInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    height: Dp = 34.dp,
    keyboardType: KeyboardType,
    isError: Boolean = false
) {
    val borderColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
    }
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, borderColor)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                textStyle = textStyle,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                style = textStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun SensorSelectorButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.label_sensor),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
