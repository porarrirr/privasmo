package com.porarrirr.sumahohikakuku.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.model.MAX_DEVICES
import com.porarrirr.sumahohikakuku.ui.SensorComparisonActions
import com.porarrirr.sumahohikakuku.ui.common.resolve
import com.porarrirr.sumahohikakuku.viewmodel.PresetListItem
import com.porarrirr.sumahohikakuku.viewmodel.SensorComparisonUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PresetSaveSheet(
    uiState: SensorComparisonUiState,
    actions: SensorComparisonActions
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val defaultDevice = stringResource(R.string.label_device_default_name)
    val unselected = stringResource(R.string.label_unselected)

    ModalBottomSheet(
        onDismissRequest = actions.closePresetSheet,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_preset_save_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = actions.closePresetSheet) {
                    Text(stringResource(R.string.action_close))
                }
            }

            if (uiState.isPresetProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text(
                text = stringResource(R.string.label_preset_save_target_device),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (uiState.devices.isEmpty()) {
                Text(
                    text = stringResource(R.string.text_no_operable_devices),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                var deviceMenuExpanded by remember { mutableStateOf(false) }
                val targetDevice = uiState.presetTargetDevice
                val targetLabel = targetDevice?.name?.ifBlank { defaultDevice } ?: unselected
                ExposedDropdownMenuBox(
                    expanded = deviceMenuExpanded,
                    onExpandedChange = { deviceMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = targetLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_preset_save_target_device_field)) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .bringIntoViewOnFocus(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = deviceMenuExpanded)
                        },
                        enabled = !uiState.isPresetProcessing
                    )
                    DropdownMenu(
                        expanded = deviceMenuExpanded,
                        onDismissRequest = { deviceMenuExpanded = false }
                    ) {
                        uiState.devices.forEach { device ->
                            DropdownMenuItem(
                                text = { Text(device.name.ifBlank { defaultDevice }) },
                                onClick = {
                                    actions.updatePresetTargetDevice(device.id)
                                    deviceMenuExpanded = false
                                },
                                enabled = !uiState.isPresetProcessing
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.description_selected_device_saved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = uiState.presetNameInput,
                onValueChange = actions.updatePresetNameInput,
                label = { Text(stringResource(R.string.label_preset_new_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(),
                singleLine = true,
                enabled = !uiState.isPresetProcessing
            )

            Button(
                onClick = actions.savePreset,
                enabled = uiState.isPresetSaveEnabled && !uiState.isPresetProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.button_save_as_preset))
            }

            uiState.presetErrorMessage?.let { errorMessage ->
                val context = LocalContext.current
                Text(
                    text = errorMessage.resolve(context),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PresetLibrarySheet(
    uiState: SensorComparisonUiState,
    actions: SensorComparisonActions
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val defaultDevice = stringResource(R.string.label_device_default_name)
    val unselected = stringResource(R.string.label_unselected)
    val targetLabel = uiState.presetTargetDevice?.name?.ifBlank { defaultDevice } ?: unselected
    val canAppendDevice = uiState.devices.size < MAX_DEVICES

    ModalBottomSheet(
        onDismissRequest = actions.closePresetSheet,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.title_preset_library),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = actions.closePresetSheet) {
                    Text(stringResource(R.string.action_close))
                }
            }

            if (uiState.isPresetProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text(
                text = stringResource(R.string.label_preset_overwrite_target_device),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (uiState.devices.isEmpty()) {
                Text(
                    text = stringResource(R.string.text_no_overwrite_target_devices),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                var deviceMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = deviceMenuExpanded,
                    onExpandedChange = { deviceMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = targetLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_preset_overwrite_target_device)) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .bringIntoViewOnFocus(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = deviceMenuExpanded)
                        },
                        enabled = !uiState.isPresetProcessing
                    )
                    DropdownMenu(
                        expanded = deviceMenuExpanded,
                        onDismissRequest = { deviceMenuExpanded = false }
                    ) {
                        uiState.devices.forEach { device ->
                            DropdownMenuItem(
                                text = { Text(device.name.ifBlank { defaultDevice }) },
                                onClick = {
                                    actions.updatePresetTargetDevice(device.id)
                                    deviceMenuExpanded = false
                                },
                                enabled = !uiState.isPresetProcessing
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.description_preset_add_or_overwrite),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!canAppendDevice) {
                Text(
                    text = stringResource(R.string.text_max_devices_reached_hint, MAX_DEVICES),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            uiState.presetErrorMessage?.let { errorMessage ->
                val context = LocalContext.current
                Text(
                    text = errorMessage.resolve(context),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider()

            Text(
                text = stringResource(
                    R.string.title_saved_presets_with_count,
                    uiState.presetListItems.size
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (uiState.presetListItems.isEmpty()) {
                Text(
                    text = stringResource(R.string.text_no_presets_registered),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(uiState.presetListItems, key = { it.id }) { item ->
                        val targetId = uiState.presetTargetDeviceId
                        val activeId = targetId?.let { uiState.activePresetAssignments[it] }
                        PresetEntryCard(
                            item = item,
                            isActive = activeId == item.id,
                            isProcessing = uiState.isPresetProcessing,
                            canAppendDevice = canAppendDevice,
                            canOverwriteTargetDevice = uiState.devices.isNotEmpty(),
                            overwriteTargetLabel = targetLabel,
                            dateFormat = dateFormat,
                            onLoad = actions.loadPreset,
                            onOverwriteTargetDeviceFromPreset = actions.overwriteTargetDeviceFromPreset,
                            onDelete = actions.deletePreset,
                            onRename = actions.renamePreset
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PresetEntryCard(
    item: PresetListItem,
    isActive: Boolean,
    isProcessing: Boolean,
    canAppendDevice: Boolean,
    canOverwriteTargetDevice: Boolean,
    overwriteTargetLabel: String,
    dateFormat: SimpleDateFormat,
    onLoad: (String) -> Unit,
    onOverwriteTargetDeviceFromPreset: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit
) {
    var renameValue by remember(item.id) { mutableStateOf(item.name) }
    LaunchedEffect(item.name) {
        renameValue = item.name
    }
    val trimmedRename = renameValue.trim()
    val canRename = trimmedRename.isNotEmpty() && trimmedRename != item.name && !isProcessing
    val updatedLabel = if (item.updatedAtEpochMillis <= 0L) {
        stringResource(R.string.label_last_updated_unknown)
    } else {
        stringResource(
            R.string.label_last_updated,
            dateFormat.format(Date(item.updatedAtEpochMillis))
        )
    }
    val fallbackColor = MaterialTheme.colorScheme.primary
    val deviceColor = remember(item.colorHex, fallbackColor) {
        runCatching { Color(AndroidColor.parseColor(item.colorHex)) }
            .getOrElse { fallbackColor }
    }
    val deviceNameLabel = item.deviceName.ifBlank { stringResource(R.string.label_untitled_device) }
    var showOverwriteDialog by remember(item.id) { mutableStateOf(false) }
    val canOverwrite = canOverwriteTargetDevice && !isProcessing

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = renameValue,
                onValueChange = { renameValue = it.take(40) },
                label = { Text(stringResource(R.string.label_preset_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(),
                singleLine = true,
                enabled = !isProcessing
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(deviceColor)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.label_device_name_with_value,
                            deviceNameLabel
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(
                            R.string.label_lens_count_with_value,
                            item.lensCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = updatedLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isActive) {
                    Text(
                        text = stringResource(R.string.label_source_for_target),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onLoad(item.id) },
                    enabled = !isProcessing && canAppendDevice,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_add_device_as_device))
                }
                OutlinedButton(
                    onClick = { showOverwriteDialog = true },
                    enabled = canOverwrite,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_overwrite))
                }
            }

            if (showOverwriteDialog) {
                AlertDialog(
                    onDismissRequest = { showOverwriteDialog = false },
                    title = { Text(stringResource(R.string.dialog_title_overwrite)) },
                    text = {
                        Text(
                            text = stringResource(
                                R.string.dialog_overwrite_target_message,
                                overwriteTargetLabel
                            )
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showOverwriteDialog = false
                                onOverwriteTargetDeviceFromPreset(item.id)
                            },
                            enabled = canOverwrite
                        ) {
                            Text(stringResource(R.string.dialog_confirm_overwrite))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showOverwriteDialog = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onRename(item.id, trimmedRename) },
                    enabled = canRename,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_rename))
                }
                FilledTonalIconButton(
                    onClick = { onDelete(item.id) },
                    enabled = !isProcessing,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.content_desc_delete_preset)
                    )
                }
            }
        }
    }
}
