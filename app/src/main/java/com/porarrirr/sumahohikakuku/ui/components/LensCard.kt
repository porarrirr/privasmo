package com.porarrirr.sumahohikakuku.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.domain.input.sanitizeDecimalInput
import com.porarrirr.sumahohikakuku.model.MANUAL_INPUT_SENSOR_VALUE
import com.porarrirr.sumahohikakuku.model.SensorSpec
import com.porarrirr.sumahohikakuku.model.isValidManualSensorDescriptor
import com.porarrirr.sumahohikakuku.ui.SensorComparisonActions
import com.porarrirr.sumahohikakuku.viewmodel.LensInputState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LensCard(
    deviceId: Long,
    lens: LensInputState,
    availableSensors: List<SensorSpec>,
    actions: SensorComparisonActions,
    canRemove: Boolean
) {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.label_lens),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(
                    onClick = { actions.removeLens(deviceId, lens.id) },
                    enabled = canRemove,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.content_desc_delete_lens)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = lens.nativeFocalLength,
                    onValueChange = {
                        actions.updateLensFocalLength(deviceId, lens.id, sanitizeDecimalInput(it))
                    },
                    label = { Text(stringResource(R.string.label_focal_length)) },
                    modifier = Modifier
                        .weight(1f)
                        .bringIntoViewOnFocus(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = lens.fNumber,
                    onValueChange = {
                        actions.updateLensFNumber(deviceId, lens.id, sanitizeDecimalInput(it))
                    },
                    label = { Text(stringResource(R.string.label_f_number)) },
                    modifier = Modifier
                        .weight(1f)
                        .bringIntoViewOnFocus(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            val selectedSensor = availableSensors.firstOrNull { it.value == lens.selectedSensorValue }
            var isSensorPickerOpen by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = isSensorPickerOpen,
                onExpandedChange = { isSensorPickerOpen = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = when {
                        selectedSensor == null -> stringResource(R.string.label_manual_input)
                        selectedSensor.isManual -> stringResource(R.string.label_manual_input)
                        else -> selectedSensor.name
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_sensor)) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .bringIntoViewOnFocus(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSensorPickerOpen)
                    }
                )
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
                OutlinedTextField(
                    value = lens.manualSensorDescriptor,
                    onValueChange = { actions.updateLensManualDescriptor(deviceId, lens.id, it) },
                    label = { Text(stringResource(R.string.label_manual_input_example)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus(),
                    singleLine = true,
                    isError = !manualValid,
                    supportingText = {
                        if (!manualValid) {
                            Text(stringResource(R.string.helper_invalid_fraction_format))
                        }
                    }
                )
            }
        }
    }
}
