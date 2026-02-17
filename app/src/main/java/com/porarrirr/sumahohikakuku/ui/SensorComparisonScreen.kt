package com.porarrirr.sumahohikakuku.ui

import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.MotionEvent
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior  
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.porarrirr.sumahohikakuku.GraphSettingsActivity
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.data.GraphSettings
import com.porarrirr.sumahohikakuku.data.GraphSettingsRepository
import com.porarrirr.sumahohikakuku.domain.input.parseHexColor
import com.porarrirr.sumahohikakuku.domain.input.sanitizeDecimalInput
import com.porarrirr.sumahohikakuku.domain.input.sanitizeHexInput
import kotlinx.coroutines.flow.distinctUntilChanged
import com.porarrirr.sumahohikakuku.model.ComparisonResults
import com.porarrirr.sumahohikakuku.model.FocalLengthMetrics
import com.porarrirr.sumahohikakuku.model.MAX_DEVICES
import com.porarrirr.sumahohikakuku.model.MAX_LENSES_PER_DEVICE
import com.porarrirr.sumahohikakuku.model.MANUAL_INPUT_SENSOR_VALUE
import com.porarrirr.sumahohikakuku.model.ProcessedDevice
import com.porarrirr.sumahohikakuku.model.isValidManualSensorDescriptor
import com.porarrirr.sumahohikakuku.viewmodel.DeviceInputState
import com.porarrirr.sumahohikakuku.viewmodel.LensInputState
import com.porarrirr.sumahohikakuku.viewmodel.PresetListItem
import com.porarrirr.sumahohikakuku.viewmodel.SensorComparisonEvent
import com.porarrirr.sumahohikakuku.viewmodel.SensorComparisonUiState
import com.porarrirr.sumahohikakuku.viewmodel.SensorComparisonViewModel
import com.porarrirr.sumahohikakuku.ui.common.resolve
import kotlin.math.max
import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val SENSOR_VIZ_SCALE = 6f
private const val FOCAL_EPSILON = 0.01

private val DEVICE_COLOR_PALETTE = listOf(
    "#2563EB",
    "#1D4ED8",
    "#1E40AF",
    "#7C3AED",
    "#9333EA",
    "#A855F7",
    "#6366F1",
    "#818CF8",
    "#A5B4FC",
    "#0EA5E9",
    "#22D3EE",
    "#38BDF8",
    "#14B8A6",
    "#10B981",
    "#34D399",
    "#059669",
    "#22C55E",
    "#4ADE80",
    "#DC2626",
    "#F97316",
    "#F59E0B",
    "#FB923C",
    "#EC4899",
    "#F472B6",
    "#FB7185"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorComparisonScreen(viewModel: SensorComparisonViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var isExporting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val graphSettingsRepository = remember(context) {
        GraphSettingsRepository(context.applicationContext)
    }
    val graphSettings by graphSettingsRepository.settingsFlow.collectAsStateWithLifecycle(
        initialValue = GraphSettings()
    )
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val actions = remember(viewModel) {
        SensorComparisonActions(
            openPresetSave = viewModel::openPresetSave,
            openPresetLibrary = viewModel::openPresetLibrary,
            closePresetSheet = viewModel::closePresetSheet,
            addDevice = viewModel::addDevice,
            consumeDeviceFocusRequest = viewModel::consumeDeviceFocusRequest,
            removeDevice = viewModel::removeDevice,
            updateDeviceName = viewModel::updateDeviceName,
            updateDeviceColor = viewModel::updateDeviceColor,
            addLens = viewModel::addLens,
            removeLens = viewModel::removeLens,
            updateLensFocalLength = viewModel::updateLensFocalLength,
            updateLensSensorSelection = viewModel::updateLensSensorSelection,
            updateLensManualDescriptor = viewModel::updateLensManualDescriptor,
            updateLensFNumber = viewModel::updateLensFNumber,
            generateComparison = viewModel::generateComparison,
            updateFocalLength = viewModel::updateFocalLength,
            updatePresetNameInput = viewModel::updatePresetNameInput,
            updatePresetTargetDevice = viewModel::updatePresetTargetDevice,
            savePreset = viewModel::savePreset,
            loadPreset = viewModel::loadPreset,
            overwriteTargetDeviceFromPreset = viewModel::overwriteTargetDeviceFromPreset,
            deletePreset = viewModel::deletePreset,
            renamePreset = viewModel::renamePreset
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SensorComparisonEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message.resolve(context))
                }
            }
        }
    }

    fun exportResultsImage(share: Boolean) {
        val results = uiState.comparisonResults ?: return
        val selectedFocalLength = uiState.selectedFocalLength
        val settingsSnapshot = graphSettings
        isExporting = true
        coroutineScope.launch {
            try {
                val activity = context.findComponentActivity() ?: error("Activity not found")
                val exportedAt = System.currentTimeMillis()
                val widthPx = context.resources.displayMetrics.widthPixels
                val bitmap = renderComposableToBitmap(activity, widthPx) {
                    ExportComparisonImageContent(
                        results = results,
                        graphSettings = settingsSnapshot,
                        selectedFocalLength = selectedFocalLength,
                        exportedAtEpochMillis = exportedAt
                    )
                }
                val fileName = "sumahohikakuku_" +
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(exportedAt)) +
                    ".png"
                val uri = try {
                    saveBitmapToPictures(context, bitmap, fileName)
                } finally {
                    bitmap.recycle()
                }

                if (share) {
                    shareImage(context, uri)
                } else {
                    android.widget.Toast
                        .makeText(
                            context,
                            context.getString(R.string.toast_image_saved),
                            android.widget.Toast.LENGTH_SHORT
                        )
                        .show()
                }
            } catch (error: Throwable) {
                val prefixRes = if (share) {
                    R.string.error_failed_to_create_share_image
                } else {
                    R.string.error_failed_to_save_image
                }
                val prefix = context.getString(prefixRes)
                android.widget.Toast
                    .makeText(context, "$prefix: ${error.localizedMessage}", android.widget.Toast.LENGTH_LONG)
                    .show()
            } finally {
                isExporting = false
            }
        }
    }

    androidx.compose.material3.Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.sensor_comparison_title)) },
                actions = {
                    TextButton(
                        onClick = actions.openPresetSave,
                        enabled = uiState.devices.isNotEmpty() && !uiState.isPresetProcessing
                    ) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_save))
                    }
                    TextButton(
                        onClick = actions.openPresetLibrary,
                        enabled = !uiState.isPresetProcessing
                    ) {
                        Text(stringResource(R.string.action_load))
                    }
                    IconButton(
                        onClick = {
                            val intent = android.content.Intent(context, GraphSettingsActivity::class.java)
                            if (context !is android.app.Activity) {
                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.menu_settings)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        if (isExporting) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Text(
            text = "高度なスマートフォンセンサー比較ツール (総光量対応)",  
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "センサーリストから選択するか、『手動入力』を選んで 1/1.28 のような分数表記で入力してください。(センサー比率は 4:3 を前提)",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "デバイス",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${uiState.devices.size} / $MAX_DEVICES",
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "比較対象に追加したいスマホを登録します。",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = actions.addDevice,
                    enabled = uiState.canAddDevice,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_add_device))
                }
            }
        }

        DeviceConfigSection(
            uiState = uiState,
            focusDeviceId = uiState.deviceFocusRequestId,
            actions = actions
        )

        Button(
            onClick = actions.generateComparison,
            enabled = uiState.isGenerateEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.button_generate_graph))
        }

        uiState.comparisonResults?.let { results ->
            ResultsSection(
                uiState = uiState,
                results = results,
                graphSettings = graphSettings,
                isExporting = isExporting,
                onFocalLengthChanged = actions.updateFocalLength,
                onExportResultsImage = ::exportResultsImage
            )
        }
    }
    }

    when (uiState.presetSheet) {
        com.porarrirr.sumahohikakuku.viewmodel.PresetSheet.SAVE -> {
            PresetSaveSheet(
                uiState = uiState,
                actions = actions
            )
        }
        com.porarrirr.sumahohikakuku.viewmodel.PresetSheet.LIBRARY -> {
            PresetLibrarySheet(
                uiState = uiState,
                actions = actions
            )
        }
        com.porarrirr.sumahohikakuku.viewmodel.PresetSheet.NONE -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetSummaryCard(
    uiState: SensorComparisonUiState,
    onSelectTargetDevice: (Long?) -> Unit,
    onOpenSave: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    val targetDevice = uiState.presetTargetDevice
    val activePresetId = targetDevice?.let { device -> uiState.activePresetAssignments[device.id] }
    val activePresetName = activePresetId?.let { id -> uiState.presets.firstOrNull { it.id == id }?.name }
    val deviceLabel = targetDevice?.name?.ifBlank { "無題のデバイス" } ?: "未選択"
    Card(
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "プリセット",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "対象デバイス1台分の構成を保存 / 呼び出しできます。",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    var deviceMenuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = deviceMenuExpanded,
                        onExpandedChange = { deviceMenuExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = deviceLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.label_target_device)) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .bringIntoViewOnFocus(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = deviceMenuExpanded)
                            },
                            enabled = uiState.devices.isNotEmpty() && !uiState.isPresetProcessing
                        )
                        DropdownMenu(
                            expanded = deviceMenuExpanded,
                            onDismissRequest = { deviceMenuExpanded = false }
                        ) {
                            uiState.devices.forEach { device ->
                                DropdownMenuItem(
                                    text = { Text(device.name.ifBlank { "デバイス" }) },
                                    onClick = {
                                        onSelectTargetDevice(device.id)
                                        deviceMenuExpanded = false
                                    },
                                    enabled = !uiState.isPresetProcessing
                                )
                            }
                        }
                    }
                    Text(
                        text = "このデバイスの元プリセット: ${activePresetName ?: "なし"}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "保存済み: ${uiState.presets.size} 件",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenSave,
                        enabled = uiState.devices.isNotEmpty() && !uiState.isPresetProcessing
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                    OutlinedButton(onClick = onOpenLibrary, enabled = !uiState.isPresetProcessing) {
                        Text(stringResource(R.string.action_list))
                    }
                }
            }
            if (uiState.isPresetProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetSaveSheet(
    uiState: SensorComparisonUiState,
    actions: SensorComparisonActions
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
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
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (uiState.devices.isEmpty()) {
                Text(
                    text = "操作できるデバイスがありません。先にデバイスを追加してください。",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                var deviceMenuExpanded by remember { mutableStateOf(false) }
                val targetDevice = uiState.presetTargetDevice
                val targetLabel = targetDevice?.name?.ifBlank { "デバイス" } ?: "未選択"
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
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deviceMenuExpanded) },
                        enabled = !uiState.isPresetProcessing
                    )
                    DropdownMenu(
                        expanded = deviceMenuExpanded,
                        onDismissRequest = { deviceMenuExpanded = false }
                    ) {
                        uiState.devices.forEach { device ->
                            DropdownMenuItem(
                                text = { Text(device.name.ifBlank { "デバイス" }) },
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
                    text = "選択したデバイスの構成を保存します。",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = uiState.presetNameInput,
                onValueChange = actions.updatePresetNameInput,
                label = { Text(stringResource(R.string.label_preset_new_name)) },
                modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
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
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetLibrarySheet(
    uiState: SensorComparisonUiState,
    actions: SensorComparisonActions
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN) }
    val targetLabel = uiState.presetTargetDevice?.name?.ifBlank { "デバイス" } ?: "未選択"
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
                    text = "プリセット一覧",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
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
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (uiState.devices.isEmpty()) {
                Text(
                    text = "上書きできるデバイスがありません。先にデバイスを追加してください。",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
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
                                text = { Text(device.name.ifBlank { "デバイス" }) },
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
                text = "「追加」は比較対象に新しいデバイスとして追加します。\n" +
                    "「上書き」は上書き先デバイスの設定を置き換えます。",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!canAppendDevice) {
                Text(
                    text = "端末数が上限です (最大${MAX_DEVICES}台)。削除してから読み込んでください。",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
            }

            uiState.presetErrorMessage?.let { errorMessage ->
                val context = LocalContext.current
                Text(
                    text = errorMessage.resolve(context),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider()

            Text(
                text = "保存済みプリセット (${uiState.presetListItems.size})",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (uiState.presetListItems.isEmpty()) {
                Text(
                    text = "まだプリセットは登録されていません。",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
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
private fun PresetEntryCard(
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
    val updatedLabel = remember(item.updatedAtEpochMillis) {
        if (item.updatedAtEpochMillis <= 0L) {
            "最終更新: -"
        } else {
            "最終更新: ${dateFormat.format(Date(item.updatedAtEpochMillis))}"
        }
    }
    val fallbackColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val deviceColor = remember(item.colorHex, fallbackColor) {
        runCatching { Color(AndroidColor.parseColor(item.colorHex)) }
            .getOrElse { fallbackColor }
    }
    val deviceNameLabel = item.deviceName.ifBlank { "無題のデバイス" }
    var showOverwriteDialog by remember(item.id) { mutableStateOf(false) }
    val canOverwrite = canOverwriteTargetDevice && !isProcessing

    Card(
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = renameValue,
                onValueChange = { renameValue = it.take(40) },
                label = { Text(stringResource(R.string.label_preset_name)) },
                modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
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
                        .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline, CircleShape)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "端末名: $deviceNameLabel",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "レンズ数: ${item.lensCount}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
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
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isActive) {
                    Text(
                        text = "対象の元",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
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
                            text = "上書き先: $overwriteTargetLabel\n" +
                                "端末名・色・レンズ設定が置き換わります。"
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
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "プリセットを削除")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceConfigSection(
    uiState: SensorComparisonUiState,
    focusDeviceId: Long?,
    actions: SensorComparisonActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (uiState.devices.isNotEmpty()) {
            val configuration = LocalConfiguration.current
            val cardWidth = remember(configuration) {
                (configuration.screenWidthDp.dp - 32.dp).coerceAtLeast(320.dp)
            }
            val listState = rememberLazyListState()
            val focusManager = LocalFocusManager.current
            LaunchedEffect(focusDeviceId) {
                val id = focusDeviceId ?: return@LaunchedEffect
                val index = uiState.devices.indexOfFirst { it.id == id }
                if (index >= 0) {
                    listState.animateScrollToItem(index)
                }
                actions.consumeDeviceFocusRequest()
            }
            LaunchedEffect(listState) {
                snapshotFlow { listState.isScrollInProgress }
                    .distinctUntilChanged()
                    .collect { isScrolling ->
                        if (isScrolling) {
                            focusManager.clearFocus()
                        }
                    }
            }
            val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
            val nestedScrollConnection = remember(listState.canScrollBackward, listState.canScrollForward, uiState.devices.size) {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        val horizontal = available.x
                        val vertical = available.y
                        if (abs(horizontal) <= abs(vertical)) return Offset.Zero
                        val canScroll = when {
                            horizontal > 0f -> listState.canScrollBackward
                            horizontal < 0f -> listState.canScrollForward
                            else -> true
                        }
                        if (!canScroll) {
                            return Offset(horizontal, 0f)
                        }
                        return Offset.Zero
                    }
                }
            }
            val activeIndex by remember {
                derivedStateOf { listState.firstVisibleItemIndex }
            }
            if (uiState.devices.size > 1) {
                ScrollHintBanner(
                    showSwipeHint = listState.canScrollForward || listState.canScrollBackward
                )
            }
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .nestedScroll(nestedScrollConnection),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        flingBehavior = flingBehavior,
                        userScrollEnabled = uiState.devices.size > 1
                    ) {
                        items(uiState.devices, key = { it.id }) { device ->
                            Box(
                                modifier = Modifier.width(cardWidth),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                DeviceCard(
                                    device = device,
                                    availableSensors = uiState.availableSensors,
                                    availableColors = uiState.availableDeviceColors,
                                    actions = actions,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (uiState.devices.size > 1 && listState.canScrollForward) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(32.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                                    )
                                )
                        )
                    }
                    if (uiState.devices.size > 1 && listState.canScrollBackward) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .width(32.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f), Color.Transparent)
                                    )
                                )
                        )
                    }
                }
            }
            if (uiState.devices.size > 1) {
                DeviceCarouselIndicator(
                    activeIndex = activeIndex,
                    itemCount = uiState.devices.size
                )
            }
        }
    }
}

@Composable
private fun ScrollHintBanner(showSwipeHint: Boolean, modifier: Modifier = Modifier) {
    if (!showSwipeHint) return
    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Text(
                text = "左右にスワイプしてデバイスを切り替えられます",
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun DeviceCarouselIndicator(
    activeIndex: Int,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    if (itemCount <= 1) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(itemCount) { index ->
            val isActive = index == activeIndex
            val color = if (isActive) {
                androidx.compose.material3.MaterialTheme.colorScheme.primary
            } else {
                androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            }
            Box(
                modifier = Modifier
                    .size(if (isActive) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun DeviceCard(
    device: DeviceInputState,
    availableSensors: List<com.porarrirr.sumahohikakuku.model.SensorSpec>,
    availableColors: List<String>,
    actions: SensorComparisonActions,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = device.name.ifBlank { "デバイス" },
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(
                    onClick = { actions.removeDevice(device.id) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "デバイスを削除")
                }
            }

            OutlinedTextField(
                value = device.name,
                onValueChange = { actions.updateDeviceName(device.id, it) },
                label = { Text(stringResource(R.string.label_name)) },
                modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus()
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
                OutlinedButton(onClick = { actions.addLens(device.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.button_add_lens_with_max, MAX_LENSES_PER_DEVICE))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LensCard(
    deviceId: Long,
    lens: LensInputState,
    availableSensors: List<com.porarrirr.sumahohikakuku.model.SensorSpec>,
    actions: SensorComparisonActions,
    canRemove: Boolean
) {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.label_lens),
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(
                    onClick = { actions.removeLens(deviceId, lens.id) },
                    enabled = canRemove,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "レンズを削除")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = lens.nativeFocalLength,
                    onValueChange = {
                        actions.updateLensFocalLength(deviceId, lens.id, sanitizeDecimalInput(it))
                    },
                    label = { Text(stringResource(R.string.label_focal_length)) },
                    modifier = Modifier.weight(1f).bringIntoViewOnFocus(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = lens.fNumber,
                    onValueChange = {
                        actions.updateLensFNumber(deviceId, lens.id, sanitizeDecimalInput(it))
                    },
                    label = { Text(stringResource(R.string.label_f_number)) },
                    modifier = Modifier.weight(1f).bringIntoViewOnFocus(),
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
                    value = selectedSensor?.name ?: "手動入力",
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
                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SensorPickerSheet(
    availableSensors: List<com.porarrirr.sumahohikakuku.model.SensorSpec>,
    selectedSensorValue: String,
    onSelectSensor: (String) -> Unit,
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var manufacturerFilter by remember { mutableStateOf<String?>(null) }

    val manufacturerOptions = remember(availableSensors) {
        val seen = linkedSetOf<String>()
        availableSensors.forEach { sensor ->
            if (!sensor.isManual && sensor.manufacturer.isNotBlank()) {
                seen.add(sensor.manufacturer)
            }
        }
        seen.toList()
    }

    val filteredSensors = remember(availableSensors, query, manufacturerFilter) {
        val tokens = query.trim()
            .replace('　', ' ')
            .lowercase(Locale.US)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        availableSensors.filter { sensor ->
            if (sensor.isManual) return@filter true
            if (manufacturerFilter != null && sensor.manufacturer != manufacturerFilter) {
                return@filter false
            }
            if (tokens.isEmpty()) return@filter true
            val haystack = "${sensor.name} ${sensor.manufacturer}".lowercase(Locale.US)
            tokens.all { token -> haystack.contains(token) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "センサー選択",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.action_close))
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.label_search)) },
                placeholder = { Text(stringResource(R.string.placeholder_search_sensor)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                trailingIcon = {
                    if (query.isNotBlank()) {
                        TextButton(onClick = { query = "" }) {
                            Text(stringResource(R.string.action_clear))
                        }
                    }
                }
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = manufacturerFilter == null,
                        onClick = { manufacturerFilter = null },
                        label = { Text(stringResource(R.string.label_all)) }
                    )
                }
                items(manufacturerOptions, key = { it }) { manufacturer ->
                    FilterChip(
                        selected = manufacturerFilter == manufacturer,
                        onClick = {
                            manufacturerFilter =
                                if (manufacturerFilter == manufacturer) null else manufacturer
                        },
                        label = { Text(manufacturer) }
                    )
                }
            }

            HorizontalDivider()

            if (filteredSensors.isEmpty()) {
                Text(
                    text = "該当するセンサーがありません",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.75f)
                ) {
                    items(filteredSensors, key = { it.value }) { sensor ->
                        val isSelected = sensor.value == selectedSensorValue
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = sensor.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                onSelectSensor(sensor.value)
                                onClose()
                            },
                            trailingIcon = {
                                if (isSelected) {
                                    Text(
                                        text = "選択中",
                                        fontSize = 12.sp,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSelector(
    selectedColor: String,
    availableColors: List<String>,
    onSelectColor: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.label_color), style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
        var isPaletteDialogOpen by remember { mutableStateOf(false) }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 6
        ) {
            availableColors.forEach { colorHex ->
                val color = Color(AndroidColor.parseColor(colorHex))
                val borderColor = if (colorHex.equals(selectedColor, ignoreCase = true)) androidx.compose.material3.MaterialTheme.colorScheme.primary else Color.Transparent
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color = color, shape = RoundedCornerShape(6.dp))
                        .border(BorderStroke(2.dp, borderColor), shape = RoundedCornerShape(6.dp))
                        .padding(2.dp)
                        .background(color.copy(alpha = 0.9f), shape = RoundedCornerShape(4.dp))
                        .padding(1.dp)
                        .clickableWithoutRipple { onSelectColor(colorHex.uppercase(Locale.US)) }
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
            modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
            isError = !isValidHex,
            supportingText = {
                if (!isValidHex) {
                    Text(stringResource(R.string.helper_hex_six_digits))
                }
            },
            trailingIcon = {
                val previewColor = runCatching { Color(AndroidColor.parseColor(customColorInput)) }.getOrNull()
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = previewColor ?: Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            },
            singleLine = true
        )

        if (isPaletteDialogOpen) {
            ColorPaletteDialog(
                selectedColor = selectedColor,
                onSelectColor = onSelectColor,
                onDismiss = { isPaletteDialogOpen = false }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPaletteDialog(
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
                        BorderStroke(3.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
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

@Composable
private fun ResultsSection(
    uiState: SensorComparisonUiState,
    results: ComparisonResults,
    graphSettings: GraphSettings,
    isExporting: Boolean,
    onFocalLengthChanged: (Double) -> Unit,
    onExportResultsImage: (Boolean) -> Unit
) {
    val focalLengths = results.focalLengths
    if (focalLengths.isEmpty()) return
    val selectedIndex = focalLengths.indexOf(uiState.selectedFocalLength).takeIf { it >= 0 } ?: 0
    val sliderSteps = (focalLengths.size - 2).coerceAtLeast(0)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.section_comparison_graph),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onExportResultsImage(false) },
                enabled = !isExporting,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.action_save_results_image))
            }
            OutlinedButton(
                onClick = { onExportResultsImage(true) },
                enabled = !isExporting,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.action_share_results_image))
            }
        }

        ChartsSection(results = results, graphSettings = graphSettings)

        Text(
            text = stringResource(R.string.section_interactive),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
        )
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "焦点距離 (35mm換算): ${formatFocalLength(uiState.selectedFocalLength)}mm",
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = selectedIndex.toFloat(),
                    onValueChange = {
                        val index = it.roundToInt().coerceIn(0, focalLengths.lastIndex)
                        onFocalLengthChanged(focalLengths[index])
                    },
                    valueRange = 0f..focalLengths.lastIndex.toFloat(),
                    steps = sliderSteps,
                    colors = SliderDefaults.colors(thumbColor = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                )
            }
        }

        Text(
            text = stringResource(R.string.section_sensor_details),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(results.devices) { device ->
                DeviceMetricsCard(
                    device = device,
                    focalLength = uiState.selectedFocalLength,
                    modifier = Modifier
                        .widthIn(min = 260.dp, max = 360.dp)
                )
            }
        }
    }
}

@Composable
private fun ChartsSection(
    results: ComparisonResults,
    graphSettings: GraphSettings
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ChartCard(
            title = "有効センサー面積の比較",
            results = results,
            graphSettings = graphSettings,
            yLabel = "有効センサー面積",
            yUnit = "mm²",
            yDecimals = 2,
            dataExtractor = { metric -> metric.effectiveAreaSqMm.toFloat() }
        )
        ChartCard(
            title = "集光力 (有効面積 / F値²) の比較",
            results = results,
            graphSettings = graphSettings,
            yLabel = "集光力",
            yDecimals = 3,
            dataExtractor = { metric -> metric.totalLightIntake.toFloat() }
        )
    }
}

@Composable
private fun ExportComparisonImageContent(
    results: ComparisonResults,
    graphSettings: GraphSettings,
    selectedFocalLength: Double,
    exportedAtEpochMillis: Long
) {
    val exportedAtLabel = remember(exportedAtEpochMillis) {
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date(exportedAtEpochMillis))
    }

    Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "スマートフォンセンサー比較",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "作成: $exportedAtLabel",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "比較グラフ",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            ChartsSection(results = results, graphSettings = graphSettings)

            Text(
                text = "スペック表 (${formatFocalLength(selectedFocalLength)}mm)",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            ExportSpecTable(results = results, focalLength = selectedFocalLength)
        }
    }
}

@Composable
private fun ExportSpecTable(
    results: ComparisonResults,
    focalLength: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        results.devices.forEach { device ->
            val metrics = device.metricsAt(focalLength) ?: return@forEach
            val fallbackColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
            val deviceColor = remember(device.colorHex, fallbackColor) {
                runCatching { Color(AndroidColor.parseColor(device.colorHex)) }
                    .getOrElse { fallbackColor }
            }

            val sensorMetrics = metrics.baseLens.sensorMetrics
            val binning = sensorMetrics.binningType
            val nativePixelPitch = sensorMetrics.nativePixelSizeUm.takeIf { it > 0 }
                ?.let { "${it.format(2)} µm" }
            val binnedPixelPitch = computeBinnedPixelSize(sensorMetrics.nativePixelSizeUm, binning)
                ?.let { "${it.format(2)} µm" }
            val pixelPitchLabel = if (binnedPixelPitch != null) "画素ピッチ (ビニング後)" else "画素ピッチ"
            val pixelPitchValue = binnedPixelPitch ?: nativePixelPitch ?: "N/A"

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(deviceColor)
                                .border(
                                    1.dp,
                                    androidx.compose.material3.MaterialTheme.colorScheme.outline,
                                    CircleShape
                                )
                        )
                        Text(
                            text = device.name,
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = deviceColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedCard(
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                        )
                    ) {
                        DeviceMetricsKeyValueTable(
                            items = listOf(
                                "有効面積" to "${metrics.effectiveAreaSqMm.format(2)} mm²",
                                pixelPitchLabel to pixelPitchValue,
                                "集光力" to metrics.totalLightIntake.format(3),
                                "実焦点距離" to "${metrics.baseLens.actualFocalLengthMm.format(2)} mm",
                                "F値" to "F${metrics.baseLens.fNumber.format(2)}",
                                "使用センサー" to sensorMetrics.sensorName
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    results: ComparisonResults,
    graphSettings: GraphSettings,
    yLabel: String,
    yUnit: String? = null,
    yDecimals: Int = 2,
    dataExtractor: (FocalLengthMetrics) -> Float
) {
    val isDarkTheme = isSystemInDarkTheme()
    val axisTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (isDarkTheme) 0.2f else 0.08f
    ).toArgb()
    val chartSurfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.toArgb()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    LineChart(context).apply {
                        description.isEnabled = false
                        axisRight.isEnabled = false
                        setDrawGridBackground(false)
                        setDrawBorders(false)
                        setTouchEnabled(true)
                        setPinchZoom(true)
                        setHighlightPerTapEnabled(true)
                        setHighlightPerDragEnabled(true)
                        setOnChartGestureListener(object : OnChartGestureListener {
                            override fun onChartLongPressed(me: MotionEvent?) {
                                me ?: return
                                getHighlightByTouchPoint(me.x, me.y)?.let { highlight ->
                                    highlightValue(highlight, true)
                                }
                            }

                            override fun onChartGestureStart(
                                me: MotionEvent?,
                                lastPerformedGesture: ChartTouchListener.ChartGesture?
                            ) = Unit

                            override fun onChartGestureEnd(
                                me: MotionEvent?,
                                lastPerformedGesture: ChartTouchListener.ChartGesture?
                            ) = Unit

                            override fun onChartDoubleTapped(me: MotionEvent?) = Unit

                            override fun onChartSingleTapped(me: MotionEvent?) = Unit

                            override fun onChartFling(
                                me1: MotionEvent?,
                                me2: MotionEvent?,
                                velocityX: Float,
                                velocityY: Float
                            ) = Unit

                            override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) = Unit

                            override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) = Unit
                        })
                        marker = ChartMarkerView(context, yLabel = yLabel, yUnit = yUnit, yDecimals = yDecimals).also {
                            it.chartView = this
                        }
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.granularity = 1f
                        xAxis.setDrawGridLines(false)
                        xAxis.setDrawAxisLine(false)
                        xAxis.valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                return formatFocalLength(value.toDouble())
                            }
                        }
                        axisLeft.setDrawGridLines(true)
                        axisLeft.setLabelCount(4, false)
                        axisLeft.setDrawAxisLine(false)
                        axisLeft.axisMinimum = 0f
                        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                        legend.orientation = Legend.LegendOrientation.HORIZONTAL
                        legend.setDrawInside(false)
                    }
                },
                update = { chart ->
                    chart.xAxis.textColor = axisTextColor
                    chart.xAxis.textSize = 12f
                    chart.axisLeft.textColor = axisTextColor
                    chart.axisLeft.textSize = 12f
                    chart.axisLeft.gridColor = gridColor
                    chart.legend.textColor = axisTextColor
                    chart.legend.textSize = 12f
                    chart.legend.form = Legend.LegendForm.CIRCLE
                    chart.legend.formSize = 10f

                    val axisLabelDecimals = yDecimals.coerceAtMost(2)
                    chart.axisLeft.valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val formatted = String.format(Locale.US, "%.${axisLabelDecimals}f", value)
                            val unitSuffix = yUnit?.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
                            return "$formatted$unitSuffix"
                        }
                    }

                    val fillTopAlpha = when (results.devices.size) {
                        1 -> 80
                        2 -> 64
                        else -> 52
                    }
                    val glowAlpha = if (isDarkTheme) 64 else 40

                    val dataSets = mutableListOf<ILineDataSet>()
                    val legendEntries = mutableListOf<LegendEntry>()
                    results.devices.forEach { device ->
                        val parsedColor = runCatching { AndroidColor.parseColor(device.colorHex) }
                            .getOrDefault(AndroidColor.DKGRAY)
                        val nativeFocals = device.lenses
                            .map { lens -> lens.nativeFocalLength35mm }
                            .distinct()

                        val markerDrawable = createLensMarkerDrawable(
                            context = chart.context,
                            accentColor = parsedColor,
                            surfaceColor = chartSurfaceColor
                        )

                        val entries = results.focalLengths.mapNotNull { focal ->
                            val metrics = device.metricsAt(focal) ?: return@mapNotNull null
                            Entry(focal.toFloat(), dataExtractor(metrics)).apply {
                                data = metrics
                                if (isApproximatelyIn(focal, nativeFocals)) {
                                    icon = markerDrawable
                                }
                            }
                        }

                        dataSets += LineDataSet(entries, "").apply {
                            color = withAlpha(parsedColor, glowAlpha)
                            setDrawValues(false)
                            setDrawCircles(false)
                            setDrawFilled(false)
                            setDrawIcons(false)
                            lineWidth = graphSettings.glowLineWidth
                            mode = LineDataSet.Mode.LINEAR
                            isHighlightEnabled = false
                        }

                        dataSets += LineDataSet(entries, device.name).apply {
                            color = parsedColor
                            highLightColor = parsedColor
                            setDrawValues(false)
                            setDrawCircles(false)
                            setDrawFilled(true)
                            setDrawIcons(true)
                            lineWidth = graphSettings.lineWidth
                            mode = LineDataSet.Mode.LINEAR
                            fillDrawable = createLineFillDrawable(
                                lineColor = parsedColor,
                                topAlpha = fillTopAlpha
                            )
                        }

                        legendEntries += LegendEntry().apply {
                            label = device.name
                            formColor = parsedColor
                            form = Legend.LegendForm.CIRCLE
                        }
                    }

                    chart.legend.setCustom(legendEntries)
                    chart.data = LineData(dataSets)
                    chart.axisLeft.setDrawLabels(true)
                    chart.axisLeft.axisMinimum = 0f
                    chart.invalidate()
                }
            )
        }
    }
}

private fun createLineFillDrawable(lineColor: Int, topAlpha: Int): GradientDrawable {
    return GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(withAlpha(lineColor, topAlpha), withAlpha(lineColor, 0))
    )
}

private fun createLensMarkerDrawable(
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
        com.porarrirr.sumahohikakuku.R.drawable.ic_lens_marker
    )?.mutate()?.also { DrawableCompat.setTint(it, accentColor) }

    return LayerDrawable(arrayOf(background, lens ?: background)).apply {
        setLayerInset(1, insetPx, insetPx, insetPx, insetPx)
        setBounds(0, 0, sizePx, sizePx)
    }
}

private fun withAlpha(color: Int, alpha: Int): Int {
    return AndroidColor.argb(
        alpha.coerceIn(0, 255),
        AndroidColor.red(color),
        AndroidColor.green(color),
        AndroidColor.blue(color)
    )
}

@Composable
private fun DeviceMetricsCard(
    device: ProcessedDevice,
    focalLength: Double,
    modifier: Modifier = Modifier
) {
    val metrics = device.metricsAt(focalLength) ?: return
    val fallbackColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val color = remember(device.colorHex, fallbackColor) {
        runCatching { Color(AndroidColor.parseColor(device.colorHex)) }.getOrElse { fallbackColor }
    }
    var isDetailsExpanded by remember(device.name, device.colorHex) { mutableStateOf(false) }

    val sensorMetrics = metrics.baseLens.sensorMetrics
    val binning = sensorMetrics.binningType
    val nativePixelPitch = sensorMetrics.nativePixelSizeUm.takeIf { it > 0 }?.let { "${it.format(2)} µm" }
    val binnedPixelPitch = computeBinnedPixelSize(sensorMetrics.nativePixelSizeUm, binning)?.let { "${it.format(2)} µm" }
    val pixelPitchLabel = if (binnedPixelPitch != null) "画素ピッチ (ビニング後)" else "画素ピッチ"
    val pixelPitchValue = binnedPixelPitch ?: nativePixelPitch ?: "N/A"
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = device.name, color = color, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            SensorVisualization(metrics = metrics, borderColor = color)

            DeviceMetricsHighlightGrid(
                items = listOf(
                    "有効面積" to "${metrics.effectiveAreaSqMm.format(2)} mm²",
                    pixelPitchLabel to pixelPitchValue,
                    "集光力" to metrics.totalLightIntake.format(3)
                )
            )

            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background)
            ) {
                DeviceMetricsKeyValueTable(
                    items = listOf(
                        "有効寸法" to "${metrics.effectiveWidthMm.format(2)} × ${metrics.effectiveHeightMm.format(2)} mm",
                        "実焦点距離" to "${metrics.baseLens.actualFocalLengthMm.format(2)} mm",
                        "F値" to "F${metrics.baseLens.fNumber.format(2)}",
                        "有効口径" to "${metrics.apertureDiameterMm.format(2)} mm",
                        "デジタルズーム" to "${metrics.zoomRatio.format(2)}x",
                        "使用センサー" to sensorMetrics.sensorName,
                        "使用レンズ" to "${metrics.baseLens.nativeFocalLength35mm.format(1)}mm (35mm換算)"
                    ),
                    modifier = Modifier.padding(12.dp)
                )
            }

            val detailItems = buildList {
                add("ビニング特性" to binning)
                nativePixelPitch?.let { add("ネイティブ画素ピッチ" to it) }
                binnedPixelPitch?.let { add("実効画素ピッチ" to it) }
                add("開口面積" to "${metrics.apertureAreaSqMm.format(2)} mm²")
            }

            TextButton(
                onClick = { isDetailsExpanded = !isDetailsExpanded },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (isDetailsExpanded) "詳細を隠す ▲" else "詳細を表示 ▼")
            }

            if (isDetailsExpanded) {
                OutlinedCard(
                    colors = CardDefaults.outlinedCardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background)
                ) {
                    DeviceMetricsKeyValueTable(
                        items = detailItems,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceMetricsHighlightGrid(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    val safeItems = items.filter { it.first.isNotBlank() && it.second.isNotBlank() }
    if (safeItems.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val rows = safeItems.chunked(2)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { (label, value) ->
                    MetricHighlightTile(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricHighlightTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DeviceMetricsKeyValueTable(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    val safeItems = items.filter { it.first.isNotBlank() && it.second.isNotBlank() }
    Column(modifier = modifier) {
        safeItems.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (index != safeItems.lastIndex) {
                HorizontalDivider(
                    color = androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun SensorVisualization(metrics: FocalLengthMetrics, borderColor: Color) {
    val widthDp = max(metrics.effectiveWidthMm.toFloat() * SENSOR_VIZ_SCALE, 2f).dp
    val heightDp = max(metrics.effectiveHeightMm.toFloat() * SENSOR_VIZ_SCALE, 2f).dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(widthDp, heightDp)
                .border(2.dp, borderColor, RoundedCornerShape(4.dp))
                .background(Color.LightGray, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {}
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.label_dimensions_width_height,
                metrics.effectiveWidthMm,
                metrics.effectiveHeightMm
            ),
            fontSize = 12.sp
        )
    }
}

private fun computeBinnedPixelSize(nativePixelSize: Double, binningType: String): Double? {
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

private fun Double.format(decimals: Int): String {
    return String.format(Locale.US, "%.${decimals}f", this)
}

private fun formatFocalLength(value: Double): String {
    val rounded = value.roundToInt().toDouble()
    return if (kotlin.math.abs(value - rounded) < FOCAL_EPSILON) {
        rounded.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

private fun isApproximatelyIn(value: Double, candidates: List<Double>): Boolean {
    return candidates.any { candidate -> kotlin.math.abs(candidate - value) < FOCAL_EPSILON }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.bringIntoViewOnFocus(): Modifier = composed {
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
private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.clickable(indication = null, interactionSource = interactionSource) { onClick() }
}
