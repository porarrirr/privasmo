package com.porarrirr.sumahohikakuku.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.model.MAX_LENSES_PER_DEVICE
import com.porarrirr.sumahohikakuku.model.SensorSpec
import com.porarrirr.sumahohikakuku.ui.SensorComparisonActions
import com.porarrirr.sumahohikakuku.viewmodel.DeviceInputState

@Composable
internal fun DeviceCard(
    device: DeviceInputState,
    availableSensors: List<SensorSpec>,
    availableColors: List<String>,
    actions: SensorComparisonActions,
    modifier: Modifier = Modifier
) {
    val defaultDeviceName = stringResource(R.string.label_device_default_name)
    var showDeleteConfirmation by remember { mutableStateOf(false) }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Device color dot in the header
                val headerColor = runCatching { Color(AndroidColor.parseColor(device.colorHex)) }
                    .getOrElse { MaterialTheme.colorScheme.primary }
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(headerColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Text(
                    text = device.name.ifBlank { defaultDeviceName },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(
                    onClick = { showDeleteConfirmation = true },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.content_desc_delete_device)
                    )
                }
            }

            OutlinedTextField(
                value = device.name,
                onValueChange = { actions.updateDeviceName(device.id, it) },
                label = { Text(stringResource(R.string.label_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus()
            )

            ColorSelector(
                selectedColor = device.colorHex,
                availableColors = availableColors,
                onSelectColor = { actions.updateDeviceColor(device.id, it) }
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                device.lenses.forEach { lens ->
                    key(lens.id) {
                        LensCard(
                            deviceId = device.id,
                            lens = lens,
                            availableSensors = availableSensors,
                            actions = actions,
                            canRemove = device.lenses.size > 1
                        )
                    }
                }
            }

            if (device.lenses.size < MAX_LENSES_PER_DEVICE) {
                OutlinedButton(
                    onClick = { actions.addLens(device.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.button_add_lens_with_max, MAX_LENSES_PER_DEVICE))
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.dialog_delete_device_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dialog_delete_device_message,
                        device.name.ifBlank { defaultDeviceName }
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        actions.removeDevice(device.id)
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
