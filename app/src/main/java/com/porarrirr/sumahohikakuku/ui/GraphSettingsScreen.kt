package com.porarrirr.sumahohikakuku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.data.CustomSensorRepository
import com.porarrirr.sumahohikakuku.data.CustomSensorRepositoryError
import com.porarrirr.sumahohikakuku.data.GraphSettings
import com.porarrirr.sumahohikakuku.data.GraphSettingsRepository
import com.porarrirr.sumahohikakuku.domain.input.sanitizeDecimalInput
import com.porarrirr.sumahohikakuku.model.CustomSensorEntry
import com.porarrirr.sumahohikakuku.model.SensorSpec
import com.porarrirr.sumahohikakuku.model.normalizeBinning
import com.porarrirr.sumahohikakuku.model.parseSensorCsv
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { GraphSettingsRepository(context.applicationContext) }
    val sensorRepository = remember(context) { CustomSensorRepository(context.applicationContext) }
    val settings by repository.settingsFlow.collectAsStateWithLifecycle(initialValue = GraphSettings())
    val customSensors by sensorRepository.sensorsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var lineWidth by remember { mutableFloatStateOf(settings.lineWidth) }
    LaunchedEffect(settings.lineWidth) {
        lineWidth = settings.lineWidth
    }

    var exportAspectWidthInput by remember {
        mutableStateOf(settings.exportAspectWidth.toString())
    }
    var exportAspectHeightInput by remember {
        mutableStateOf(settings.exportAspectHeight.toString())
    }
    LaunchedEffect(settings.exportAspectWidth, settings.exportAspectHeight) {
        exportAspectWidthInput = settings.exportAspectWidth.toString()
        exportAspectHeightInput = settings.exportAspectHeight.toString()
    }

    var builtInSensors by remember { mutableStateOf<List<SensorSpec>>(emptyList()) }
    LaunchedEffect(Unit) {
        builtInSensors = withContext(Dispatchers.IO) {
            runCatching {
                val raw = context.resources.openRawResource(R.raw.sensor_database)
                    .bufferedReader()
                    .use { it.readText() }
                parseSensorCsv(raw)
            }.getOrElse { emptyList() }
        }
    }

    LaunchedEffect(sensorRepository) {
        sensorRepository.errors.collectLatest { error ->
            val messageRes = when (error) {
                is CustomSensorRepositoryError.WriteFailed -> R.string.error_failed_to_save_custom_sensors
                else -> R.string.error_failed_to_load_custom_sensors
            }
            snackbarHostState.showSnackbar(context.getString(messageRes))
        }
    }

    var isEditorOpen by remember { mutableStateOf(false) }
    var editorTarget by remember { mutableStateOf<CustomSensorEntry?>(null) }
    var deleteTarget by remember { mutableStateOf<CustomSensorEntry?>(null) }
    val existingNames by remember(customSensors, builtInSensors, editorTarget) {
        derivedStateOf {
            val editingId = editorTarget?.id
            val customNames = customSensors
                .filterNot { it.id == editingId }
                .map { it.name.trim().lowercase(Locale.US) }
            val builtInNames = builtInSensors.map { it.name.trim().lowercase(Locale.US) }
            (customNames + builtInNames).toSet()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                    )
                )
            )
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = stringResource(R.string.label_chart_line_width), fontWeight = FontWeight.SemiBold)
                        Text(text = String.format(Locale.US, "%.1f", lineWidth))
                        Slider(
                            value = lineWidth,
                            onValueChange = { lineWidth = it },
                            onValueChangeFinished = {
                                coroutineScope.launch { repository.setLineWidth(lineWidth) }
                            },
                            valueRange = GraphSettings.MIN_LINE_WIDTH..GraphSettings.MAX_LINE_WIDTH,
                            steps = (((GraphSettings.MAX_LINE_WIDTH - GraphSettings.MIN_LINE_WIDTH) / 0.5f).toInt() - 1)
                                .coerceAtLeast(0),
                            colors = SliderDefaults.colors()
                        )
                    }
                }

                val aspectWidth = exportAspectWidthInput.toIntOrNull()
                val aspectHeight = exportAspectHeightInput.toIntOrNull()
                val isAspectValid =
                    aspectWidth != null &&
                        aspectHeight != null &&
                        aspectWidth in GraphSettings.MIN_EXPORT_ASPECT_COMPONENT..GraphSettings.MAX_EXPORT_ASPECT_COMPONENT &&
                        aspectHeight in GraphSettings.MIN_EXPORT_ASPECT_COMPONENT..GraphSettings.MAX_EXPORT_ASPECT_COMPONENT

                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_export_image_aspect_ratio),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(
                                R.string.helper_export_image_aspect_ratio_range,
                                GraphSettings.MIN_EXPORT_ASPECT_COMPONENT,
                                GraphSettings.MAX_EXPORT_ASPECT_COMPONENT
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                R.string.label_current_export_aspect_ratio,
                                settings.exportAspectWidth,
                                settings.exportAspectHeight
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    exportAspectWidthInput = "4"
                                    exportAspectHeightInput = "3"
                                    coroutineScope.launch {
                                        repository.setExportAspectRatio(width = 4, height = 3)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("4:3")
                            }
                            OutlinedButton(
                                onClick = {
                                    exportAspectWidthInput = "1"
                                    exportAspectHeightInput = "1"
                                    coroutineScope.launch {
                                        repository.setExportAspectRatio(width = 1, height = 1)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("1:1")
                            }
                            OutlinedButton(
                                onClick = {
                                    exportAspectWidthInput = "16"
                                    exportAspectHeightInput = "9"
                                    coroutineScope.launch {
                                        repository.setExportAspectRatio(width = 16, height = 9)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("16:9")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = exportAspectWidthInput,
                                onValueChange = { exportAspectWidthInput = sanitizeIntegerInput(it) },
                                label = { Text(stringResource(R.string.label_aspect_ratio_width)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = exportAspectWidthInput.isNotBlank() && (
                                    aspectWidth == null ||
                                        aspectWidth !in GraphSettings.MIN_EXPORT_ASPECT_COMPONENT..
                                        GraphSettings.MAX_EXPORT_ASPECT_COMPONENT
                                    )
                            )
                            OutlinedTextField(
                                value = exportAspectHeightInput,
                                onValueChange = { exportAspectHeightInput = sanitizeIntegerInput(it) },
                                label = { Text(stringResource(R.string.label_aspect_ratio_height)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = exportAspectHeightInput.isNotBlank() && (
                                    aspectHeight == null ||
                                        aspectHeight !in GraphSettings.MIN_EXPORT_ASPECT_COMPONENT..
                                        GraphSettings.MAX_EXPORT_ASPECT_COMPONENT
                                    )
                            )
                        }

                        Button(
                            onClick = {
                                val width = aspectWidth ?: return@Button
                                val height = aspectHeight ?: return@Button
                                coroutineScope.launch {
                                    repository.setExportAspectRatio(width = width, height = height)
                                }
                            },
                            enabled = isAspectValid,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_apply_export_image_aspect_ratio))
                        }
                    }
                }

            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = stringResource(R.string.label_custom_sensor_list), fontWeight = FontWeight.SemiBold)
                    Text(text = stringResource(R.string.helper_custom_sensor_editor))

                    OutlinedButton(
                        onClick = {
                            editorTarget = null
                            isEditorOpen = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(R.string.action_add))
                    }

                    if (customSensors.isEmpty()) {
                        Text(text = stringResource(R.string.text_no_custom_sensors))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            customSensors
                                .sortedBy { it.name }
                                .forEach { entry ->
                                    Card {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(text = entry.name, fontWeight = FontWeight.Medium)
                                            Text(
                                                text = String.format(
                                                    Locale.US,
                                                    "%.2f MP / %.2f µm / %s",
                                                    entry.megapixels,
                                                    entry.pixelSizeUm,
                                                    entry.binningType
                                                )
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        editorTarget = entry
                                                        isEditorOpen = true
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Edit,
                                                        contentDescription = null
                                                    )
                                                    Text(stringResource(R.string.action_edit))
                                                }
                                                OutlinedButton(
                                                    onClick = { deleteTarget = entry },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Delete,
                                                        contentDescription = null
                                                    )
                                                    Text(stringResource(R.string.action_delete))
                                                }
                                            }
                                        }
                                    }
                                }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        repository.reset()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_reset_default))
            }
        }
    }
    }

    if (isEditorOpen) {
        CustomSensorEditorDialog(
            initial = editorTarget,
            existingNames = existingNames,
            onDismiss = { isEditorOpen = false },
            onSave = { entry ->
                coroutineScope.launch {
                    sensorRepository.upsertSensor(entry)
                }
                isEditorOpen = false
            }
        )
    }

    val pendingDelete = deleteTarget
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.dialog_delete_sensor_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dialog_delete_sensor_message,
                        pendingDelete.name
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            sensorRepository.deleteSensor(pendingDelete.id)
                        }
                        deleteTarget = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

private fun sanitizeIntegerInput(value: String): String {
    return value.filter { it.isDigit() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomSensorEditorDialog(
    initial: CustomSensorEntry?,
    existingNames: Set<String>,
    onDismiss: () -> Unit,
    onSave: (CustomSensorEntry) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var megapixelsInput by remember(initial) {
        mutableStateOf(
            initial?.megapixels?.let { String.format(Locale.US, "%.2f", it) }.orEmpty()
        )
    }
    var pixelSizeInput by remember(initial) {
        mutableStateOf(
            initial?.pixelSizeUm?.let { String.format(Locale.US, "%.2f", it) }.orEmpty()
        )
    }
    val normalizedInitialBinning = remember(initial) {
        normalizeBinning(initial?.binningType.orEmpty()).ifBlank { "Unknown" }
    }
    val binningOptions = remember(normalizedInitialBinning) {
        val known = listOf(
            "None",
            "Quad Bayer (2x2)",
            "Nona (3x3)",
            "16-cell (4x4)",
            "Unknown"
        )
        (listOfNotNull(normalizedInitialBinning.takeIf { it !in known }) + known).distinct()
    }
    var binningSelection by remember(initial) { mutableStateOf(normalizedInitialBinning) }
    var isBinningMenuExpanded by remember { mutableStateOf(false) }

    val normalizedName = name.trim()
    val normalizedMp = sanitizeDecimalInput(megapixelsInput).toDoubleOrNull()
    val normalizedPixel = sanitizeDecimalInput(pixelSizeInput).toDoubleOrNull()
    val isDuplicate = normalizedName.isNotBlank() &&
        normalizedName.lowercase(Locale.US) in existingNames
    val isValid = normalizedName.isNotBlank() &&
        normalizedMp != null && normalizedMp > 0.0 &&
        normalizedPixel != null && normalizedPixel > 0.0 &&
        !isDuplicate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    stringResource(R.string.title_add_sensor)
                } else {
                    stringResource(R.string.title_edit_sensor)
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_sensor_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isDuplicate
                )
                if (isDuplicate) {
                    Text(
                        text = stringResource(R.string.error_duplicate_sensor_name),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error
                    )
                }
                OutlinedTextField(
                    value = megapixelsInput,
                    onValueChange = { megapixelsInput = sanitizeDecimalInput(it) },
                    label = { Text(stringResource(R.string.label_megapixels)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = pixelSizeInput,
                    onValueChange = { pixelSizeInput = sanitizeDecimalInput(it) },
                    label = { Text(stringResource(R.string.label_pixel_size_um)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                ExposedDropdownMenuBox(
                    expanded = isBinningMenuExpanded,
                    onExpandedChange = { isBinningMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = binningSelection,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_binning)) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isBinningMenuExpanded)
                        },
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = isBinningMenuExpanded,
                        onDismissRequest = { isBinningMenuExpanded = false }
                    ) {
                        binningOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    binningSelection = option
                                    isBinningMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entry = CustomSensorEntry(
                        id = initial?.id ?: "custom:${UUID.randomUUID()}",
                        name = normalizedName,
                        megapixels = normalizedMp ?: 0.0,
                        pixelSizeUm = normalizedPixel ?: 0.0,
                        binningType = binningSelection.trim().ifBlank { "Unknown" }
                    )
                    onSave(entry)
                },
                enabled = isValid
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
