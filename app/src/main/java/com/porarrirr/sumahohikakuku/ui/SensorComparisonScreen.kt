package com.porarrirr.sumahohikakuku.ui

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.porarrirr.sumahohikakuku.GraphSettingsActivity
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.data.GraphSettings
import com.porarrirr.sumahohikakuku.data.GraphSettingsRepository
import com.porarrirr.sumahohikakuku.model.MAX_DEVICES
import com.porarrirr.sumahohikakuku.ui.common.resolve
import com.porarrirr.sumahohikakuku.ui.components.ChartCard
import com.porarrirr.sumahohikakuku.ui.components.DeviceCard
import com.porarrirr.sumahohikakuku.ui.components.ExportChartImageContent
import com.porarrirr.sumahohikakuku.ui.components.ExportChartType
import com.porarrirr.sumahohikakuku.ui.components.PresetLibrarySheet
import com.porarrirr.sumahohikakuku.ui.components.PresetSaveSheet
import com.porarrirr.sumahohikakuku.ui.components.formatFocalLength
import com.porarrirr.sumahohikakuku.viewmodel.PresetSheet
import com.porarrirr.sumahohikakuku.viewmodel.SensorComparisonEvent
import com.porarrirr.sumahohikakuku.viewmodel.SensorComparisonUiState
import com.porarrirr.sumahohikakuku.viewmodel.SensorComparisonViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private enum class SensorComparisonTab(@StringRes val titleRes: Int) {
    DEVICE_INPUT(R.string.tab_device_input),
    COMPARISON_GRAPH(R.string.tab_comparison_graph)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorComparisonScreen(viewModel: SensorComparisonViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isExporting by remember { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val graphSettingsRepository = remember(context) {
        GraphSettingsRepository(context.applicationContext)
    }
    val graphSettings by graphSettingsRepository.settingsFlow.collectAsStateWithLifecycle(
        initialValue = GraphSettings()
    )
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val inputScrollState = rememberScrollState()
    val graphScrollState = rememberScrollState()
    val tabs = remember { SensorComparisonTab.entries }
    var selectedDeviceId by rememberSaveable { mutableStateOf<Long?>(null) }
    val currentTab = tabs.getOrElse(selectedTabIndex) { SensorComparisonTab.DEVICE_INPUT }
    var graphRangeIndices by remember(uiState.comparisonResults?.focalLengths) {
        val maxIndex = (uiState.comparisonResults?.focalLengths?.lastIndex ?: 0).toFloat()
        mutableStateOf(0f..maxIndex)
    }

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
            updateLensOpticalEndFocalLength = viewModel::updateLensOpticalEndFocalLength,
            updateLensEndFNumber = viewModel::updateLensEndFNumber,
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

    LaunchedEffect(uiState.devices) {
        selectedDeviceId = selectedDeviceId
            ?.takeIf { id -> uiState.devices.any { it.id == id } }
            ?: uiState.devices.firstOrNull()?.id
    }

    LaunchedEffect(uiState.deviceFocusRequestId, uiState.devices) {
        val focusId = uiState.deviceFocusRequestId ?: return@LaunchedEffect
        if (uiState.devices.any { it.id == focusId }) {
            selectedDeviceId = focusId
        }
        actions.consumeDeviceFocusRequest()
    }

    LaunchedEffect(selectedTabIndex, tabs.size) {
        if (selectedTabIndex !in tabs.indices) {
            selectedTabIndex = 0
        }
    }

    LaunchedEffect(currentTab, uiState.devices, uiState.availableSensors) {
        if (currentTab == SensorComparisonTab.COMPARISON_GRAPH) {
            actions.generateComparison()
        }
    }

    fun shareChartImage(chartType: ExportChartType) {
        val results = uiState.comparisonResults ?: return
        val focalLengths = results.focalLengths
        if (focalLengths.isEmpty()) return
        val graphStartIndex = graphRangeIndices.start.roundToInt()
            .coerceIn(0, focalLengths.lastIndex)
        val graphEndIndex = graphRangeIndices.endInclusive.roundToInt()
            .coerceIn(graphStartIndex, focalLengths.lastIndex)
        val graphFocalRange = focalLengths[graphStartIndex]..focalLengths[graphEndIndex]

        val settingsSnapshot = graphSettings
        isExporting = true
        coroutineScope.launch {
            try {
                val activity = context.findComponentActivity()
                    ?: throw IllegalStateException(
                        context.getString(R.string.error_activity_not_found)
                    )
                val exportedAt = System.currentTimeMillis()
                val widthPx = context.resources.displayMetrics.widthPixels
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(Date(exportedAt))
                val rawBitmap = renderComposableToBitmap(activity, widthPx) {
                    ExportChartImageContent(
                        results = results,
                        graphSettings = settingsSnapshot,
                        chartType = chartType,
                        focalRange = graphFocalRange,
                        exportedAtEpochMillis = exportedAt
                    )
                }
                val aspectBitmap = fitBitmapToAspectRatio(
                    source = rawBitmap,
                    aspectWidth = settingsSnapshot.exportAspectWidth,
                    aspectHeight = settingsSnapshot.exportAspectHeight
                )
                val fileName = "sumahohikakuku_${timestamp}_${chartType.fileSuffix}.png"
                val uri = try {
                    saveBitmapToShareCache(context, aspectBitmap, fileName)
                } finally {
                    if (aspectBitmap !== rawBitmap) {
                        rawBitmap.recycle()
                    }
                    aspectBitmap.recycle()
                }
                shareImages(context, listOf(uri), context.getString(R.string.title_share_image))
            } catch (error: Throwable) {
                val prefix = context.getString(R.string.error_failed_to_create_share_image)
                android.widget.Toast
                    .makeText(
                        context,
                        "$prefix: ${error.localizedMessage}",
                        android.widget.Toast.LENGTH_LONG
                    )
                    .show()
            } finally {
                isExporting = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MainToolbar(
                title = stringResource(R.string.sensor_comparison_title),
                saveEnabled = uiState.devices.isNotEmpty() && !uiState.isPresetProcessing,
                loadEnabled = !uiState.isPresetProcessing,
                onSave = actions.openPresetSave,
                onLoad = actions.openPresetLibrary,
                onSettings = {
                    val intent = Intent(context, GraphSettingsActivity::class.java)
                    if (context !is android.app.Activity) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(innerPadding)
        ) {
            if (isExporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            IosStyleTabSelector(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onSelect = { selectedTabIndex = it }
            )

            when (currentTab) {
                SensorComparisonTab.DEVICE_INPUT -> {
                    DeviceInputTabContent(
                        uiState = uiState,
                        actions = actions,
                        selectedDeviceId = selectedDeviceId,
                        onSelectedDeviceIdChange = { selectedDeviceId = it },
                        scrollState = inputScrollState
                    )
                }

                SensorComparisonTab.COMPARISON_GRAPH -> {
                    ComparisonGraphTabContent(
                        uiState = uiState,
                        graphSettings = graphSettings,
                        isExporting = isExporting,
                        graphRangeIndices = graphRangeIndices,
                        onGraphRangeChanged = { graphRangeIndices = it },
                        scrollState = graphScrollState,
                        onShareChartImage = ::shareChartImage
                    )
                }
            }
        }
    }

    when (uiState.presetSheet) {
        PresetSheet.SAVE -> {
            PresetSaveSheet(uiState = uiState, actions = actions)
        }

        PresetSheet.LIBRARY -> {
            PresetLibrarySheet(uiState = uiState, actions = actions)
        }

        PresetSheet.NONE -> Unit
    }
}

@Composable
private fun MainToolbar(
    title: String,
    saveEnabled: Boolean,
    loadEnabled: Boolean,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToolbarIconButton(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = stringResource(R.string.action_save),
                        enabled = saveEnabled,
                        onClick = onSave
                    )
                    ToolbarIconButton(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = stringResource(R.string.action_load),
                        enabled = loadEnabled,
                        onClick = onLoad
                    )
                }

                Text(
                    text = title,
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                ToolbarIconButton(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.menu_settings),
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = onSettings
                )
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val tint = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(40.dp)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = tint
        )
    }
}

@Composable
private fun DeviceInputTabContent(
    uiState: SensorComparisonUiState,
    actions: SensorComparisonActions,
    selectedDeviceId: Long?,
    onSelectedDeviceIdChange: (Long) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val selectedDevice = uiState.devices.firstOrNull { it.id == selectedDeviceId }
        ?: uiState.devices.firstOrNull()
    val activePresetName = selectedDevice
        ?.let { device -> uiState.activePresetAssignments[device.id] }
        ?.let { presetId -> uiState.presets.firstOrNull { it.id == presetId }?.name }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DeviceSelectorPanel(
            uiState = uiState,
            selectedDeviceId = selectedDevice?.id,
            onSelectDevice = onSelectedDeviceIdChange,
            onAddDevice = actions.addDevice
        )

        if (selectedDevice != null) {
            DeviceCard(
                device = selectedDevice,
                availableSensors = uiState.availableSensors,
                availableColors = uiState.availableDeviceColors,
                activePresetName = activePresetName,
                actions = actions,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DeviceSelectorPanel(
    uiState: SensorComparisonUiState,
    selectedDeviceId: Long?,
    onSelectDevice: (Long) -> Unit,
    onAddDevice: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.tab_device_input),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${uiState.devices.size}/$MAX_DEVICES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                uiState.devices.forEach { device ->
                    DeviceSelectorChip(
                        device = device,
                        selected = device.id == selectedDeviceId,
                        onClick = { onSelectDevice(device.id) }
                    )
                }

                AddDeviceChip(
                    enabled = uiState.canAddDevice,
                    onClick = onAddDevice
                )
            }
        }
    }
}

@Composable
private fun DeviceSelectorChip(
    device: com.porarrirr.sumahohikakuku.viewmodel.DeviceInputState,
    selected: Boolean,
    onClick: () -> Unit
) {
    val fallbackColor = MaterialTheme.colorScheme.primary
    val deviceColor = remember(device.colorHex, fallbackColor) {
        runCatching { Color(android.graphics.Color.parseColor(device.colorHex)) }
            .getOrElse { fallbackColor }
    }
    val defaultDeviceName = stringResource(R.string.label_device_default_name)

    Surface(
        modifier = Modifier
            .height(44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(deviceColor)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = device.name.ifBlank { defaultDeviceName },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.label_lens_count_compact, device.lenses.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun AddDeviceChip(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier
            .height(44.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        },
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Text(
                text = stringResource(R.string.action_add_device),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ComparisonGraphTabContent(
    uiState: SensorComparisonUiState,
    graphSettings: GraphSettings,
    isExporting: Boolean,
    graphRangeIndices: ClosedFloatingPointRange<Float>,
    onGraphRangeChanged: (ClosedFloatingPointRange<Float>) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
    onShareChartImage: (ExportChartType) -> Unit
) {
    val results = uiState.comparisonResults
    if (results == null) {
        ResultsPlaceholder(message = stringResource(R.string.message_generate_graph_first))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val focalLengths = results.focalLengths
        val graphStartIndex = graphRangeIndices.start.roundToInt()
            .coerceIn(0, focalLengths.lastIndex)
        val graphEndIndex = graphRangeIndices.endInclusive.roundToInt()
            .coerceIn(graphStartIndex, focalLengths.lastIndex)
        val graphFocalRange = focalLengths[graphStartIndex]..focalLengths[graphEndIndex]

        GraphRangeSelector(
            focalLengths = focalLengths,
            graphRangeIndices = graphRangeIndices,
            onGraphRangeChanged = onGraphRangeChanged
        )

        ChartCard(
            title = stringResource(R.string.chart_title_effective_area),
            results = results,
            graphSettings = graphSettings,
            focalRange = graphFocalRange,
            yLabel = stringResource(R.string.chart_y_label_effective_area),
            yUnit = "mm\u00B2",
            yDecimals = 2,
            dataExtractor = { metric -> metric.effectiveAreaSqMm.toFloat() }
        )

        ChartCard(
            title = stringResource(R.string.chart_title_light_intake),
            results = results,
            graphSettings = graphSettings,
            focalRange = graphFocalRange,
            yLabel = stringResource(R.string.chart_y_label_light_intake),
            yDecimals = 3,
            dataExtractor = { metric -> metric.totalLightIntake.toFloat() }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShareChartButton(
                label = stringResource(R.string.action_share_area_chart),
                enabled = !isExporting,
                modifier = Modifier.weight(1f),
                onClick = { onShareChartImage(ExportChartType.EFFECTIVE_AREA) }
            )
            ShareChartButton(
                label = stringResource(R.string.action_share_light_intake_chart),
                enabled = !isExporting,
                modifier = Modifier.weight(1f),
                onClick = { onShareChartImage(ExportChartType.LIGHT_INTAKE) }
            )
        }
    }
}

@Composable
private fun GraphRangeSelector(
    focalLengths: List<Double>,
    graphRangeIndices: ClosedFloatingPointRange<Float>,
    onGraphRangeChanged: (ClosedFloatingPointRange<Float>) -> Unit
) {
    if (focalLengths.isEmpty()) return

    val graphStartIndex = graphRangeIndices.start.roundToInt()
        .coerceIn(0, focalLengths.lastIndex)
    val graphEndIndex = graphRangeIndices.endInclusive.roundToInt()
        .coerceIn(graphStartIndex, focalLengths.lastIndex)
    val graphFocalRange = focalLengths[graphStartIndex]..focalLengths[graphEndIndex]
    val sliderSteps = (focalLengths.size - 2).coerceAtLeast(0)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.label_graph_display_range),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.label_graph_range_compact,
                    formatFocalLength(graphFocalRange.start),
                    formatFocalLength(graphFocalRange.endInclusive)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (focalLengths.size > 1) {
                RangeSlider(
                    value = graphRangeIndices,
                    onValueChange = { range ->
                        val start = range.start.roundToInt()
                            .coerceIn(0, focalLengths.lastIndex)
                        val end = range.endInclusive.roundToInt()
                            .coerceIn(0, focalLengths.lastIndex)
                        val minIndex = start.coerceAtMost(end)
                        val maxIndex = end.coerceAtLeast(start)
                        onGraphRangeChanged(minIndex.toFloat()..maxIndex.toFloat())
                    },
                    valueRange = 0f..focalLengths.lastIndex.toFloat(),
                    steps = sliderSteps,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(
                        R.string.label_left_edge,
                        formatFocalLength(graphFocalRange.start)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.label_right_edge,
                        formatFocalLength(graphFocalRange.endInclusive)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ShareChartButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.IosShare,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ResultsPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
        )
        }
    }
}

@Composable
private fun IosStyleTabSelector(
    tabs: List<SensorComparisonTab>,
    selectedTabIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = selectedTabIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(tab.titleRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
