package com.porarrirr.sumahohikakuku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.data.CustomSensorRepository
import com.porarrirr.sumahohikakuku.data.GraphSettings
import com.porarrirr.sumahohikakuku.data.GraphSettingsRepository
import com.porarrirr.sumahohikakuku.model.CustomSensorEntry
import com.porarrirr.sumahohikakuku.model.SensorSpec
import com.porarrirr.sumahohikakuku.model.normalizeBinning
import com.porarrirr.sumahohikakuku.model.parseSensorCsv
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
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

    var lineWidth by remember { mutableFloatStateOf(settings.lineWidth) }
    LaunchedEffect(settings.lineWidth) {
        lineWidth = settings.lineWidth
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
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
                    Text(text = "グラフ線の太さ", fontWeight = FontWeight.SemiBold)
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

            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "センサー一覧（カスタム）", fontWeight = FontWeight.SemiBold)
                    Text(text = "MP・ピクセルサイズを入力し、ビニングを選択して追加できます。")

                    OutlinedButton(
                        onClick = {
                            editorTarget = null
                            isEditorOpen = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Text("追加")
                    }

                    if (customSensors.isEmpty()) {
                        Text(text = "まだ登録されていません。")
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
                                                    Text("編集")
                                                }
                                                OutlinedButton(
                                                    onClick = { deleteTarget = entry },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Delete,
                                                        contentDescription = null
                                                    )
                                                    Text("削除")
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
                Text("デフォルトに戻す")
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
            title = { Text("センサーを削除") },
            text = { Text("「${pendingDelete.name}」を削除しますか？") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            sensorRepository.deleteSensor(pendingDelete.id)
                        }
                        deleteTarget = null
                    }
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTarget = null }) {
                    Text("キャンセル")
                }
            }
        )
    }
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
        title = { Text(if (initial == null) "センサー追加" else "センサー編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("センサー名") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isDuplicate
                )
                if (isDuplicate) {
                    Text(text = "同名のセンサーが既に存在します。", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    value = megapixelsInput,
                    onValueChange = { megapixelsInput = sanitizeDecimalInput(it) },
                    label = { Text("画素数 (MP)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = pixelSizeInput,
                    onValueChange = { pixelSizeInput = sanitizeDecimalInput(it) },
                    label = { Text("ピクセルサイズ (µm)") },
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
                        label = { Text("ビニング") },
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
                Text("保存")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

private fun sanitizeDecimalInput(input: String): String {
    val normalized = buildString {
        input.trim().forEach { char ->
            val ascii = when (char) {
                in '０'..'９' -> (char.code - '０'.code + '0'.code).toChar()
                '．', '。', '，', ',' -> '.'
                else -> char
            }
            if (ascii.isDigit() || ascii == '.') append(ascii)
        }
    }
    val firstDot = normalized.indexOf('.')
    if (firstDot == -1) return normalized
    return normalized.substring(0, firstDot + 1) + normalized.substring(firstDot + 1).filter { it != '.' }
}
