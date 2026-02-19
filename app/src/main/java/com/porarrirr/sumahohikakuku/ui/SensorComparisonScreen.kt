package com.porarrirr.sumahohikakuku.ui

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.porarrirr.sumahohikakuku.GraphSettingsActivity
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.data.GraphSettings
import com.porarrirr.sumahohikakuku.data.GraphSettingsRepository
import com.porarrirr.sumahohikakuku.model.MAX_DEVICES
import com.porarrirr.sumahohikakuku.ui.common.resolve
import com.porarrirr.sumahohikakuku.ui.components.ComparisonGraphSection
import com.porarrirr.sumahohikakuku.ui.components.DeviceConfigSection
import com.porarrirr.sumahohikakuku.ui.components.ExportChartImageContent
import com.porarrirr.sumahohikakuku.ui.components.ExportChartType
import com.porarrirr.sumahohikakuku.ui.components.PresetLibrarySheet
import com.porarrirr.sumahohikakuku.ui.components.PresetSaveSheet
import com.porarrirr.sumahohikakuku.ui.components.SensorDetailsSection
import kotlin.math.roundToInt
import com.porarrirr.sumahohikakuku.viewmodel.PresetSheet
import com.porarrirr.sumahohikakuku.viewmodel.SensorComparisonEvent
import com.porarrirr.sumahohikakuku.viewmodel.SensorComparisonUiState
import com.porarrirr.sumahohikakuku.viewmodel.SensorComparisonViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private enum class SensorComparisonTab(@StringRes val titleRes: Int) {
    DEVICE_INPUT(R.string.tab_device_input),
    COMPARISON_GRAPH(R.string.tab_comparison_graph),
    SENSOR_DETAILS(R.string.tab_sensor_details)
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
    val detailsScrollState = rememberScrollState()
    val tabs = remember { SensorComparisonTab.entries }

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

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == SensorComparisonTab.COMPARISON_GRAPH.ordinal) {
            actions.generateComparison()
        }
    }

    fun shareResultsImages() {
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
                val uris = ExportChartType.entries.map { chartType ->
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
                    try {
                        saveBitmapToShareCache(context, aspectBitmap, fileName)
                    } finally {
                        if (aspectBitmap !== rawBitmap) {
                            rawBitmap.recycle()
                        }
                        aspectBitmap.recycle()
                    }
                }
                shareImages(context, uris, context.getString(R.string.title_share_image))
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

    fun generateComparisonAndOpenGraph() {
        selectedTabIndex = SensorComparisonTab.COMPARISON_GRAPH.ordinal
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f)
                    )
                )
            )
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.sensor_comparison_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
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
                                val intent = Intent(context, GraphSettingsActivity::class.java)
                                if (context !is android.app.Activity) {
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
                    .padding(innerPadding)
            ) {
                if (isExporting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(stringResource(tab.titleRes)) }
                        )
                    }
                }

                when (tabs[selectedTabIndex]) {
                    SensorComparisonTab.DEVICE_INPUT -> {
                        DeviceInputTabContent(
                            uiState = uiState,
                            actions = actions,
                            scrollState = inputScrollState,
                            onGenerateComparison = ::generateComparisonAndOpenGraph
                        )
                    }

                    SensorComparisonTab.COMPARISON_GRAPH -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(graphScrollState)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TextButton(
                                onClick = actions.generateComparison,
                                enabled = uiState.isGenerateEnabled,
                                modifier = Modifier.align(Alignment.End),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.action_regenerate))
                            }

                            val results = uiState.comparisonResults
                            if (results == null) {
                                ResultsPlaceholder(
                                    message = stringResource(R.string.message_generate_graph_first)
                                )
                            } else {
                                ComparisonGraphSection(
                                    results = results,
                                    graphSettings = graphSettings,
                                    isExporting = isExporting,
                                    graphRangeIndices = graphRangeIndices,
                                    onGraphRangeChanged = { graphRangeIndices = it },
                                    onShareResultsImage = ::shareResultsImages
                                )
                            }
                        }
                    }

                    SensorComparisonTab.SENSOR_DETAILS -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(detailsScrollState)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val results = uiState.comparisonResults
                            if (results == null) {
                                ResultsPlaceholder(
                                    message = stringResource(R.string.message_generate_sensor_details_first)
                                )
                            } else {
                                SensorDetailsSection(
                                    results = results,
                                    selectedFocalLength = uiState.selectedFocalLength,
                                    onFocalLengthChanged = actions.updateFocalLength
                                )
                            }
                        }
                    }
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
private fun DeviceInputTabContent(
    uiState: SensorComparisonUiState,
    actions: SensorComparisonActions,
    scrollState: androidx.compose.foundation.ScrollState,
    onGenerateComparison: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_title_advanced),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.description_manual_input_guidance),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.section_devices),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${uiState.devices.size} / $MAX_DEVICES",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.description_devices),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = actions.addDevice,
                    enabled = uiState.canAddDevice,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
            onClick = onGenerateComparison,
            enabled = uiState.isGenerateEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.button_generate_graph))
        }
    }
}

@Composable
private fun ResultsPlaceholder(message: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}
