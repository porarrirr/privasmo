package com.porarrirr.sumahohikakuku.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
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
import com.porarrirr.sumahohikakuku.viewmodel.SensorComparisonUiState
import com.porarrirr.sumahohikakuku.viewmodel.SensorComparisonViewModel
import kotlin.math.max
import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

private const val SENSOR_VIZ_SCALE = 6f

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

@Composable
fun SensorComparisonScreen(viewModel: SensorComparisonViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        PresetSummaryCard(
            uiState = uiState,
            onOpenManager = viewModel::openPresetManager
        )

        OutlinedButton(
            onClick = viewModel::addDevice,
            enabled = uiState.canAddDevice,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("デバイス追加 (最大${MAX_DEVICES}台)")
        }

        DeviceConfigSection(
            uiState = uiState,
            onRemoveDevice = viewModel::removeDevice,
            onUpdateDeviceName = viewModel::updateDeviceName,
            onUpdateDeviceColor = viewModel::updateDeviceColor,
            onAddLens = viewModel::addLens,
            onRemoveLens = viewModel::removeLens,
            onUpdateLensFocalLength = viewModel::updateLensFocalLength,
            onUpdateLensSensorSelection = viewModel::updateLensSensorSelection,
            onUpdateLensManualDescriptor = viewModel::updateLensManualDescriptor,
            onUpdateLensFNumber = viewModel::updateLensFNumber
        )

        Button(
            onClick = viewModel::generateComparison,
            enabled = uiState.isGenerateEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("グラフ生成 / 更新")
        }

        uiState.comparisonResults?.let { results ->
            ResultsSection(
                uiState = uiState,
                results = results,
                onFocalLengthChanged = viewModel::updateFocalLength
            )
        }
    }

    if (uiState.isPresetManagerVisible) {
        PresetManagerSheet(
            uiState = uiState,
            onClose = viewModel::closePresetManager,
            onPresetNameChange = viewModel::updatePresetNameInput,
            onSelectTargetDevice = viewModel::updatePresetTargetDevice,
            onSavePreset = viewModel::savePreset,
            onLoadPreset = viewModel::loadPreset,
            onDeletePreset = viewModel::deletePreset,
            onRenamePreset = viewModel::renamePreset
        )
    }
}

@Composable
private fun PresetSummaryCard(
    uiState: SensorComparisonUiState,
    onOpenManager: () -> Unit
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "プリセット",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "対象デバイス: $deviceLabel",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "割り当てプリセット: ${activePresetName ?: "未選択"}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "保存済み: ${uiState.presets.size} 件",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onOpenManager) {
                    Text("プリセット管理")
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
private fun PresetManagerSheet(
    uiState: SensorComparisonUiState,
    onClose: () -> Unit,
    onPresetNameChange: (String) -> Unit,
    onSelectTargetDevice: (Long?) -> Unit,
    onSavePreset: () -> Unit,
    onLoadPreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    onRenamePreset: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "プリセット管理",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onClose) {
                    Text("閉じる")
                }
            }

            if (uiState.isPresetProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text(
                text = "保存・読み込み対象のデバイス",
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
                        label = { Text("保存/読み込み対象デバイス") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
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
                                    onSelectTargetDevice(device.id)
                                    deviceMenuExpanded = false
                                },
                                enabled = !uiState.isPresetProcessing
                            )
                        }
                    }
                }
                Text(
                    text = "選択したデバイスに対して操作します。",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = uiState.presetNameInput,
                onValueChange = onPresetNameChange,
                label = { Text("新しいプリセット名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isPresetProcessing
            )

            Button(
                onClick = onSavePreset,
                enabled = uiState.isPresetSaveEnabled && !uiState.isPresetProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("選択中のデバイスをプリセット保存")
            }

            uiState.presetErrorMessage?.let { errorMessage ->
                Text(
                    text = errorMessage,
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
                        val canAppendDevice = uiState.devices.size < MAX_DEVICES
                        PresetEntryCard(
                            item = item,
                            isActive = activeId == item.id,
                            isProcessing = uiState.isPresetProcessing,
                            canAppendDevice = canAppendDevice,
                            dateFormat = dateFormat,
                            onLoad = onLoadPreset,
                            onDelete = onDeletePreset,
                            onRename = onRenamePreset
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
    dateFormat: SimpleDateFormat,
    onLoad: (String) -> Unit,
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
                label = { Text("プリセット名") },
                modifier = Modifier.fillMaxWidth(),
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
                        text = "選択中",
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
                    Text("読み込む")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onRename(item.id, trimmedRename) },
                    enabled = canRename,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("名前変更")
                }
                TextButton(
                    onClick = { onDelete(item.id) },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("削除")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceConfigSection(
    uiState: SensorComparisonUiState,
    onRemoveDevice: (Long) -> Unit,
    onUpdateDeviceName: (Long, String) -> Unit,
    onUpdateDeviceColor: (Long, String) -> Unit,
    onAddLens: (Long) -> Unit,
    onRemoveLens: (Long, Long) -> Unit,
    onUpdateLensFocalLength: (Long, Long, String) -> Unit,
    onUpdateLensSensorSelection: (Long, Long, String) -> Unit,
    onUpdateLensManualDescriptor: (Long, Long, String) -> Unit,
    onUpdateLensFNumber: (Long, Long, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (uiState.devices.isNotEmpty()) {
            val configuration = LocalConfiguration.current
            val cardWidth = remember(configuration) {
                (configuration.screenWidthDp.dp - 32.dp).coerceAtLeast(320.dp)
            }
            val listState = rememberLazyListState()
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
                                    onRemoveDevice = onRemoveDevice,
                                    onUpdateDeviceName = onUpdateDeviceName,
                                    onUpdateDeviceColor = onUpdateDeviceColor,
                                    onAddLens = onAddLens,
                                    onRemoveLens = onRemoveLens,
                                    onUpdateLensFocalLength = onUpdateLensFocalLength,
                                    onUpdateLensSensorSelection = onUpdateLensSensorSelection,
                                    onUpdateLensManualDescriptor = onUpdateLensManualDescriptor,
                                    onUpdateLensFNumber = onUpdateLensFNumber,
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
    onRemoveDevice: (Long) -> Unit,
    onUpdateDeviceName: (Long, String) -> Unit,
    onUpdateDeviceColor: (Long, String) -> Unit,
    onAddLens: (Long) -> Unit,
    onRemoveLens: (Long, Long) -> Unit,
    onUpdateLensFocalLength: (Long, Long, String) -> Unit,
    onUpdateLensSensorSelection: (Long, Long, String) -> Unit,
    onUpdateLensManualDescriptor: (Long, Long, String) -> Unit,
    onUpdateLensFNumber: (Long, Long, String) -> Unit,
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
                TextButton(onClick = { onRemoveDevice(device.id) }) {
                    Text("削除")
                }
            }

            OutlinedTextField(
                value = device.name,
                onValueChange = { onUpdateDeviceName(device.id, it) },
                label = { Text("名前") },
                modifier = Modifier.fillMaxWidth()
            )

            ColorSelector(
                selectedColor = device.colorHex,
                availableColors = availableColors,
                onSelectColor = { onUpdateDeviceColor(device.id, it) }
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                device.lenses.forEach { lens ->
                    key(lens.id) {
                        LensCard(
                            deviceId = device.id,
                            lens = lens,
                            availableSensors = availableSensors,
                            onRemoveLens = onRemoveLens,
                            onUpdateLensFocalLength = onUpdateLensFocalLength,
                            onUpdateLensSensorSelection = onUpdateLensSensorSelection,
                            onUpdateLensManualDescriptor = onUpdateLensManualDescriptor,
                            onUpdateLensFNumber = onUpdateLensFNumber,
                            canRemove = device.lenses.size > 1
                        )
                    }
                }
            }

            if (device.lenses.size < MAX_LENSES_PER_DEVICE) {
                OutlinedButton(onClick = { onAddLens(device.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("レンズ追加 (最大${MAX_LENSES_PER_DEVICE}個)")
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
    onRemoveLens: (Long, Long) -> Unit,
    onUpdateLensFocalLength: (Long, Long, String) -> Unit,
    onUpdateLensSensorSelection: (Long, Long, String) -> Unit,
    onUpdateLensManualDescriptor: (Long, Long, String) -> Unit,
    onUpdateLensFNumber: (Long, Long, String) -> Unit,
    canRemove: Boolean
) {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "レンズ", style = androidx.compose.material3.MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { onRemoveLens(deviceId, lens.id) }, enabled = canRemove) {
                    Text("削除")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = lens.nativeFocalLength,
                    onValueChange = { onUpdateLensFocalLength(deviceId, lens.id, it) },
                    label = { Text("焦点距離 (35mm換算)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = lens.fNumber,
                    onValueChange = { onUpdateLensFNumber(deviceId, lens.id, it) },
                    label = { Text("F値") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            var expanded by remember { mutableStateOf(false) }
            val selectedSensor = availableSensors.firstOrNull { it.value == lens.selectedSensorValue }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedSensor?.name ?: "手動入力",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("センサー") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableSensors.forEach { sensor ->
                        DropdownMenuItem(
                            text = { Text(sensor.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = {
                                onUpdateLensSensorSelection(deviceId, lens.id, sensor.value)
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (lens.selectedSensorValue == MANUAL_INPUT_SENSOR_VALUE) {
                val manualValid = isValidManualSensorDescriptor(lens.manualSensorDescriptor)
                OutlinedTextField(
                    value = lens.manualSensorDescriptor,
                    onValueChange = { onUpdateLensManualDescriptor(deviceId, lens.id, it) },
                    label = { Text("手動入力 (例: 1/1.28)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !manualValid,
                    supportingText = {
                        if (!manualValid) {
                            Text("有効な分数形式で入力してください (例: 1/1.28)")
                        }
                    }
                )
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
        Text(text = "色", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
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
            Text("カラーパレットを開く")
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
            label = { Text("カスタム色 (#RRGGBB)") },
            isError = !isValidHex,
            supportingText = {
                if (!isValidHex) {
                    Text("6桁の16進数で入力してください")
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
        title = { Text("カラーパレット") },
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
                Text("閉じる")
            }
        }
    )
}

@Composable
private fun ResultsSection(
    uiState: SensorComparisonUiState,
    results: ComparisonResults,
    onFocalLengthChanged: (Int) -> Unit
) {
    val focalLengths = results.focalLengths
    if (focalLengths.isEmpty()) return
    val selectedIndex = focalLengths.indexOf(uiState.selectedFocalLength).takeIf { it >= 0 } ?: 0
    val sliderSteps = (focalLengths.size - 2).coerceAtLeast(0)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "2. 比較グラフ", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        ChartsSection(results = results)

        Text(text = "3. インタラクティブ操作", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "焦点距離 (35mm換算): ${uiState.selectedFocalLength}mm", fontWeight = FontWeight.Medium)
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

        Text(text = "4. 選択焦点距離でのセンサー詳細", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
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
private fun ChartsSection(results: ComparisonResults) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ChartCard(
            title = "有効センサー面積の比較",
            results = results,
            dataExtractor = { metric -> metric.effectiveAreaSqMm.toFloat() }
        )
        ChartCard(
            title = "集光力 (有効面積 / F値²) の比較",
            results = results,
            dataExtractor = { metric -> metric.totalLightIntake.toFloat() }
        )
    }
}

@Composable
private fun ChartCard(
    title: String,
    results: ComparisonResults,
    dataExtractor: (FocalLengthMetrics) -> Float
) {
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
                        setTouchEnabled(true)
                        setPinchZoom(true)
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.granularity = 1f
                        xAxis.setDrawGridLines(false)
                        axisLeft.setDrawGridLines(true)
                        axisLeft.axisMinimum = 0f
                        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                        legend.orientation = Legend.LegendOrientation.HORIZONTAL
                        legend.setDrawInside(false)
                    }
                },
                update = { chart ->
                    val dataSets = results.devices.map { device ->
                        val entries = results.focalLengths.mapNotNull { focal ->
                            val metrics = device.metricsAt(focal) ?: return@mapNotNull null
                            Entry(focal.toFloat(), dataExtractor(metrics))
                        }
                        LineDataSet(entries, device.name).apply {
                            val parsedColor = AndroidColor.parseColor(device.colorHex)
                            color = parsedColor
                            highLightColor = parsedColor
                            setDrawValues(false)
                            lineWidth = 2f
                            setDrawCircles(false)
                            setDrawFilled(true)
                            fillColor = AndroidColor.argb(48, AndroidColor.red(parsedColor), AndroidColor.green(parsedColor), AndroidColor.blue(parsedColor))
                        }
                    }
                    chart.data = LineData(dataSets)
                    chart.axisLeft.setDrawLabels(true)
                    chart.axisLeft.axisMinimum = 0f
                    chart.invalidate()
                }
            )
        }
    }
}

@Composable
private fun DeviceMetricsCard(
    device: ProcessedDevice,
    focalLength: Int,
    modifier: Modifier = Modifier
) {
    val metrics = device.metricsAt(focalLength) ?: return
    val color = Color(AndroidColor.parseColor(device.colorHex))
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = device.name, color = color, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            SensorVisualization(metrics = metrics, borderColor = color)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "有効面積: ${metrics.effectiveAreaSqMm.format(2)} mm²")
                Text(text = "有効寸法: ${metrics.effectiveWidthMm.format(2)} x ${metrics.effectiveHeightMm.format(2)} mm")
                Text(text = "使用レンズ: ${metrics.baseLens.nativeFocalLength35mm.format(1)}mm (35mm換算) / F${metrics.baseLens.fNumber.format(2)}")
                Text(text = "使用センサー: ${metrics.baseLens.sensorMetrics.sensorName}")
                metrics.baseLens.sensorMetrics.nativePixelSizeUm.takeIf { it > 0 }?.let {
                    Text(text = "ネイティブピクセル: ${it.format(2)} µm")
                }
                val binning = metrics.baseLens.sensorMetrics.binningType
                Text(text = "ビニング特性: $binning")
                computeBinnedPixelSize(metrics.baseLens.sensorMetrics.nativePixelSizeUm, binning)?.let {
                    Text(text = "実効ビニングピクセル: ${it.format(2)} µm")
                }
                Text(text = "デジタルズーム: ${metrics.zoomRatio.format(2)}x")
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "レンズ特性", fontWeight = FontWeight.SemiBold)
                Text(text = "実焦点距離: ${metrics.baseLens.actualFocalLengthMm.format(2)} mm")
                Text(text = "F値: ${metrics.baseLens.fNumber.format(2)}")
                Text(text = "有効口径: ${metrics.apertureDiameterMm.format(2)} mm")
                Text(text = "開口面積: ${metrics.apertureAreaSqMm.format(2)} mm²")
                Text(text = "集光力: ${metrics.totalLightIntake.format(3)}")
                Text(text = "有効センサー面積: ${metrics.effectiveAreaSqMm.format(2)} mm²")
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
        Text(text = "幅: ${metrics.effectiveWidthMm.format(1)} mm | 高さ: ${metrics.effectiveHeightMm.format(1)} mm", fontSize = 12.sp)
    }
}

private fun sanitizeHexInput(raw: String): String {
    if (raw.isEmpty()) return "#"
    val cleaned = raw.uppercase(Locale.US)
        .replace("#", "")
        .filter(::isHexDigit)
        .take(6)
    return if (cleaned.isEmpty()) "#" else "#${cleaned}"
}

private fun parseHexColor(input: String): String? {
    val cleaned = input.uppercase(Locale.US).removePrefix("#")
    return if (cleaned.length == 6 && cleaned.all(::isHexDigit)) "#${cleaned}" else null
}

private fun isHexDigit(char: Char): Boolean {
    return char in '0'..'9' || char in 'A'..'F'
}

private fun computeBinnedPixelSize(nativePixelSize: Double, binningType: String): Double? {
    if (nativePixelSize <= 0.0) return null
    val lower = binningType.lowercase()
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

@Composable
private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.clickable(indication = null, interactionSource = interactionSource) { onClick() }
}
