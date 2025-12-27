package com.porarrirr.sumahohikakuku.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.data.CustomSensorRepository
import com.porarrirr.sumahohikakuku.data.PresetRepository
import com.porarrirr.sumahohikakuku.model.ComparisonResults
import com.porarrirr.sumahohikakuku.model.CustomSensorEntry
import com.porarrirr.sumahohikakuku.model.DEFAULT_DEVICE_COLORS
import com.porarrirr.sumahohikakuku.model.MAX_DEVICES
import com.porarrirr.sumahohikakuku.model.MAX_LENSES_PER_DEVICE
import com.porarrirr.sumahohikakuku.model.MANUAL_INPUT_SENSOR_VALUE
import com.porarrirr.sumahohikakuku.model.PresetDeviceSnapshot
import com.porarrirr.sumahohikakuku.model.PresetLensSnapshot
import com.porarrirr.sumahohikakuku.model.PresetSnapshot
import com.porarrirr.sumahohikakuku.model.SensorSpec
import com.porarrirr.sumahohikakuku.model.calculateNativeSensorMetrics
import com.porarrirr.sumahohikakuku.model.computeProcessedDevice
import com.porarrirr.sumahohikakuku.model.isValidManualSensorDescriptor
import com.porarrirr.sumahohikakuku.model.parseSensorCsv
import com.porarrirr.sumahohikakuku.model.toSensorSpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val DEFAULT_MANUAL_SENSOR_VALUE = "1/1.33"
private const val TAG = "SensorComparisonViewModel"
private const val PREFS_NAME = "sensor_comparison_prefs"
private const val KEY_SAVED_DEVICES = "saved_devices"

private val DEFAULT_FOCAL_LENGTHS = (14..260).map { it.toDouble() }
private val HEX_REGEX = Regex("^#[0-9A-F]{6}$")

sealed interface SensorComparisonEvent {
    data class ShowMessage(val message: String) : SensorComparisonEvent
}

data class LensInputState(
    val id: Long,
    val nativeFocalLength: String,
    val selectedSensorValue: String,
    val manualSensorDescriptor: String,
    val fNumber: String
) {
    val usesManualSensor: Boolean get() = selectedSensorValue == MANUAL_INPUT_SENSOR_VALUE
}

data class DeviceInputState(
    val id: Long,
    val name: String,
    val colorHex: String,
    val lenses: List<LensInputState>
)

data class PresetListItem(
    val id: String,
    val name: String,
    val deviceName: String,
    val lensCount: Int,
    val colorHex: String,
    val updatedAtEpochMillis: Long
)

enum class PresetSheet {
    NONE,
    SAVE,
    LIBRARY
}

data class SensorComparisonUiState(
    val devices: List<DeviceInputState> = emptyList(),
    val availableSensors: List<SensorSpec> = emptyList(),
    val availableDeviceColors: List<String> = DEFAULT_DEVICE_COLORS,
    val selectedFocalLength: Double = DEFAULT_FOCAL_LENGTHS.first(),
    val comparisonResults: ComparisonResults? = null,
    val focalLengths: List<Double> = DEFAULT_FOCAL_LENGTHS,
    val presets: List<PresetSnapshot> = emptyList(),
    val presetSheet: PresetSheet = PresetSheet.NONE,
    val presetNameInput: String = "",
    val presetTargetDeviceId: Long? = null,
    val activePresetAssignments: Map<Long, String> = emptyMap(),
    val isPresetProcessing: Boolean = false,
    val presetErrorMessage: String? = null,
    val deviceFocusRequestId: Long? = null
) {
    val canAddDevice: Boolean get() = devices.size < MAX_DEVICES
    val hasResults: Boolean get() = comparisonResults != null
    val isGenerateEnabled: Boolean get() = devices.any { device ->
        device.lenses.any { lens ->
            val flValid = lens.nativeFocalLength.toDoubleOrNull()?.let { it > 0.0 } == true
            val fNumberValid = lens.fNumber.toDoubleOrNull()?.let { it > 0.0 } == true
            val sensorValid = if (lens.usesManualSensor) isValidManualSensorDescriptor(lens.manualSensorDescriptor) else true
            flValid && fNumberValid && sensorValid
        }
    }

    val presetTargetDevice: DeviceInputState? get() = presetTargetDeviceId?.let { id -> devices.firstOrNull { it.id == id } }

    val presetListItems: List<PresetListItem> get() = presets.map { snapshot ->
        PresetListItem(
            id = snapshot.id,
            name = snapshot.name,
            deviceName = snapshot.device.name.ifBlank { snapshot.name },
            lensCount = snapshot.device.lenses.size,
            colorHex = snapshot.device.colorHex.let { raw ->
                val normalized = raw.trim().uppercase(Locale.US)
                if (HEX_REGEX.matches(normalized)) normalized else DEFAULT_DEVICE_COLORS.first()
            },
            updatedAtEpochMillis = snapshot.updatedAtEpochMillis
        )
    }

    val isPresetSaveEnabled: Boolean
        get() = presetNameInput.isNotBlank() && (presetTargetDevice?.lenses?.isNotEmpty() == true)
}

internal data class ResolvedSensorSelection(
    val value: String,
    val manualDescriptor: String?
)

class SensorComparisonViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SensorComparisonUiState())
    val uiState: StateFlow<SensorComparisonUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SensorComparisonEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SensorComparisonEvent> = _events.asSharedFlow()

    private val presetRepository = PresetRepository(application)
    private val customSensorRepository = CustomSensorRepository(application)
    private val sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var nextDeviceId = 1L
    private var nextLensId = 1L
    private var hasRestoredState = false

    init {
        viewModelScope.launch {
            val baseSensors = loadBaseSensors()
            customSensorRepository.sensorsFlow
                .map { custom -> mergeSensors(baseSensors, custom) }
                .distinctUntilChanged()
                .collectLatest { sensors ->
                    if (!hasRestoredState) {
                        val restoredDevices = loadPersistedDevices(sensors)
                        val devices = restoredDevices ?: createDefaultDevices(sensors)
                        _uiState.update { current ->
                            current.copy(
                                availableSensors = sensors,
                                devices = devices,
                                presetTargetDeviceId = devices.firstOrNull()?.id
                            )
                        }
                        hasRestoredState = true
                    } else {
                        _uiState.update { current ->
                            current.copy(availableSensors = sensors)
                        }
                    }
                }
        }

        viewModelScope.launch {
            presetRepository.presetsFlow.collectLatest { snapshots ->
                _uiState.update { current ->
                    val activeAssignments = current.activePresetAssignments.filterValues { id ->
                        snapshots.any { it.id == id }
                    }
                    current.copy(
                        presets = snapshots,
                        activePresetAssignments = activeAssignments
                    )
                }
            }
        }

        viewModelScope.launch {
            uiState
                .map { it.devices }
                .distinctUntilChanged()
                .collectLatest { devices ->
                    if (!hasRestoredState) return@collectLatest
                    persistDevices(devices)
                }
        }
    }

    fun addDevice() {
        val state = _uiState.value
        if (!state.canAddDevice) return
        val color = DEFAULT_DEVICE_COLORS[state.devices.size % DEFAULT_DEVICE_COLORS.size]
        val newDevice = DeviceInputState(
            id = nextDeviceId++,
            name = "デバイス ${state.devices.size + 1}",
            colorHex = color,
            lenses = listOf(newDefaultLens())
        )
        _uiState.update {
            it.copy(
                devices = it.devices + newDevice,
                comparisonResults = null,
                presetTargetDeviceId = it.presetTargetDeviceId ?: newDevice.id,
                deviceFocusRequestId = newDevice.id,
                presetErrorMessage = null
            )
        }
    }

    fun removeDevice(deviceId: Long) {
        _uiState.update { current ->
            val updated = current.devices.filterNot { it.id == deviceId }
            val updatedAssignments = current.activePresetAssignments - deviceId
            val newTarget = when {
                updated.isEmpty() -> null
                current.presetTargetDeviceId == null -> updated.first().id
                current.presetTargetDeviceId == deviceId -> updated.first().id
                else -> current.presetTargetDeviceId
            }
            current.copy(
                devices = updated,
                comparisonResults = null,
                presetTargetDeviceId = newTarget,
                activePresetAssignments = updatedAssignments,
                presetErrorMessage = null
            )
        }
    }

    fun updateDeviceName(deviceId: Long, name: String) {
        _uiState.update { current ->
            val updatedDevices = current.devices.map { device ->
                if (device.id == deviceId) device.copy(name = name) else device
            }
            current.copy(
                devices = updatedDevices,
                comparisonResults = null,
                presetErrorMessage = null
            )
        }
    }

    fun updateDeviceColor(deviceId: Long, colorHex: String) {
        _uiState.update { current ->
            val updatedDevices = current.devices.map { device ->
                if (device.id == deviceId) device.copy(colorHex = colorHex) else device
            }
            current.copy(
                devices = updatedDevices,
                comparisonResults = null,
                presetErrorMessage = null
            )
        }
    }

    fun addLens(deviceId: Long) {
        _uiState.update { current ->
            val devices = current.devices.map { device ->
                if (device.id != deviceId) return@map device
                if (device.lenses.size >= MAX_LENSES_PER_DEVICE) return@map device
                device.copy(lenses = device.lenses + newDefaultLens())
            }
            current.copy(
                devices = devices,
                comparisonResults = null,
                presetErrorMessage = null
            )
        }
    }

    fun removeLens(deviceId: Long, lensId: Long) {
        _uiState.update { current ->
            val devices = current.devices.map { device ->
                if (device.id != deviceId) return@map device
                val updatedLenses = device.lenses.filterNot { it.id == lensId }
                device.copy(lenses = if (updatedLenses.isEmpty()) listOf(newDefaultLens()) else updatedLenses)
            }
            current.copy(
                devices = devices,
                comparisonResults = null,
                presetErrorMessage = null
            )
        }
    }

    fun openPresetSave() {
        val now = System.currentTimeMillis()
        _uiState.update { current ->
            val targetId = current.presetTargetDeviceId?.takeIf { id -> current.devices.any { it.id == id } }
                ?: current.devices.firstOrNull()?.id
            val targetName = targetId?.let { id -> current.devices.firstOrNull { it.id == id }?.name }
            val defaultName = buildDefaultPresetName(targetName, now)
            current.copy(
                presetSheet = PresetSheet.SAVE,
                presetErrorMessage = null,
                presetTargetDeviceId = targetId,
                presetNameInput = if (current.presetNameInput.isBlank()) defaultName else current.presetNameInput
            )
        }
    }

    fun openPresetLibrary() {
        _uiState.update { current ->
            val targetId = current.presetTargetDeviceId?.takeIf { id -> current.devices.any { it.id == id } }
                ?: current.devices.firstOrNull()?.id
            current.copy(
                presetSheet = PresetSheet.LIBRARY,
                presetErrorMessage = null,
                presetTargetDeviceId = targetId
            )
        }
    }

    fun closePresetSheet() {
        _uiState.update {
            it.copy(
                presetSheet = PresetSheet.NONE,
                presetNameInput = "",
                presetErrorMessage = null
            )
        }
    }

    fun consumeDeviceFocusRequest() {
        _uiState.update { it.copy(deviceFocusRequestId = null) }
    }

    fun updatePresetNameInput(newValue: String) {
        val normalized = newValue.take(40)
        _uiState.update { it.copy(presetNameInput = normalized) }
    }

    fun updatePresetTargetDevice(deviceId: Long?) {
        _uiState.update { current ->
            val resolvedId = deviceId?.takeIf { id -> current.devices.any { it.id == id } }
                ?: current.devices.firstOrNull()?.id
            current.copy(presetTargetDeviceId = resolvedId, presetErrorMessage = null)
        }
    }

    fun savePreset() {
        val state = _uiState.value
        val trimmedName = state.presetNameInput.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(presetErrorMessage = "プリセット名を入力してください") }
            return
        }
        val targetDevice = state.presetTargetDevice
        if (targetDevice == null) {
            _uiState.update { it.copy(presetErrorMessage = "保存対象のデバイスを選択してください") }
            return
        }
        val deviceSnapshot = targetDevice.toSnapshot()
        if (deviceSnapshot.lenses.isEmpty()) {
            _uiState.update { it.copy(presetErrorMessage = "保存するレンズ設定がありません") }
            return
        }
        val now = System.currentTimeMillis()
        val snapshot = PresetSnapshot(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            device = deviceSnapshot,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now
        )
        startPresetOperation()
        viewModelScope.launch {
            runCatching { presetRepository.upsertPreset(snapshot) }
                .onSuccess {
                    finishPresetOperation { current ->
                        val exists = current.devices.any { it.id == targetDevice.id }
                        val assignments = if (exists) {
                            current.activePresetAssignments + (targetDevice.id to snapshot.id)
                        } else {
                            current.activePresetAssignments
                        }
                        current.copy(
                            presetSheet = PresetSheet.NONE,
                            presetNameInput = "",
                            activePresetAssignments = assignments
                        )
                    }
                    postMessage("プリセットを保存しました: ${snapshot.name}")
                }
                .onFailure { error ->
                    failPresetOperation(error)
                }
        }
    }

    fun loadPreset(presetId: String) {
        val state = _uiState.value
        val snapshot = state.presets.firstOrNull { it.id == presetId } ?: return
        if (state.devices.size >= MAX_DEVICES) {
            _uiState.update { it.copy(presetErrorMessage = "これ以上端末を追加できません (最大${MAX_DEVICES}台)") }
            return
        }
        val newDeviceId = nextDeviceId++
        val fallbackColor = DEFAULT_DEVICE_COLORS[state.devices.size % DEFAULT_DEVICE_COLORS.size]
        val newDevice = createDeviceFromPreset(snapshot, state.availableSensors, newDeviceId, fallbackColor)
        _uiState.update { current ->
            val devices = current.devices + newDevice
            val assignments = current.activePresetAssignments + (newDeviceId to presetId)
            current.copy(
                devices = devices,
                comparisonResults = null,
                presetSheet = PresetSheet.NONE,
                presetNameInput = "",
                presetErrorMessage = null,
                presetTargetDeviceId = newDeviceId,
                deviceFocusRequestId = newDeviceId,
                activePresetAssignments = assignments
            )
        }
        postMessage("デバイスを追加しました: ${newDevice.name}")
    }

    fun overwriteTargetDeviceFromPreset(presetId: String) {
        val state = _uiState.value
        val targetDeviceId = state.presetTargetDeviceId
        if (targetDeviceId == null) {
            _uiState.update { it.copy(presetErrorMessage = "上書きするデバイスを選択してください") }
            return
        }
        val snapshot = state.presets.firstOrNull { it.id == presetId } ?: return
        val targetDevice = state.devices.firstOrNull { it.id == targetDeviceId }
        if (targetDevice == null) {
            _uiState.update { it.copy(presetErrorMessage = "上書きするデバイスが見つかりません") }
            return
        }

        val overwritten = createDeviceFromPreset(
            snapshot = snapshot,
            sensors = state.availableSensors,
            deviceId = targetDeviceId,
            fallbackColor = targetDevice.colorHex
        )

        _uiState.update { current ->
            val devices = current.devices.map { device ->
                if (device.id == targetDeviceId) overwritten else device
            }
            current.copy(
                devices = devices,
                comparisonResults = null,
                presetSheet = PresetSheet.NONE,
                presetErrorMessage = null,
                deviceFocusRequestId = targetDeviceId,
                activePresetAssignments = current.activePresetAssignments + (targetDeviceId to presetId)
            )
        }
        postMessage("デバイスを上書きしました: ${overwritten.name}")
    }

    fun deletePreset(presetId: String) {
        startPresetOperation()
        viewModelScope.launch {
            runCatching { presetRepository.deletePreset(presetId) }
                .onSuccess {
                    finishPresetOperation { current ->
                        val assignments = current.activePresetAssignments.filterValues { it != presetId }
                        current.copy(activePresetAssignments = assignments)
                    }
                    postMessage("プリセットを削除しました")
                }
                .onFailure { error -> failPresetOperation(error) }
        }
    }

    fun renamePreset(presetId: String, newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(presetErrorMessage = "新しいプリセット名を入力してください") }
            return
        }
        startPresetOperation()
        viewModelScope.launch {
            runCatching { presetRepository.updatePresetName(presetId, trimmedName.take(40)) }
                .onSuccess {
                    finishPresetOperation()
                    postMessage("プリセット名を変更しました")
                }
                .onFailure { error -> failPresetOperation(error) }
        }
    }

    fun updateLensFocalLength(deviceId: Long, lensId: Long, value: String) {
        updateLens(deviceId, lensId) { it.copy(nativeFocalLength = value) }
    }

    fun updateLensFNumber(deviceId: Long, lensId: Long, value: String) {
        updateLens(deviceId, lensId) { it.copy(fNumber = value) }
    }

    fun updateLensSensorSelection(deviceId: Long, lensId: Long, newValue: String) {
        updateLens(deviceId, lensId) { lens ->
            if (newValue == MANUAL_INPUT_SENSOR_VALUE) {
                lens.copy(selectedSensorValue = newValue, manualSensorDescriptor = lens.manualSensorDescriptor.ifBlank { DEFAULT_MANUAL_SENSOR_VALUE })
            } else {
                lens.copy(selectedSensorValue = newValue)
            }
        }
    }

    fun updateLensManualDescriptor(deviceId: Long, lensId: Long, descriptor: String) {
        updateLens(deviceId, lensId) { it.copy(manualSensorDescriptor = descriptor) }
    }

    fun updateFocalLength(focalLength: Double) {
        val state = _uiState.value
        val coerced = focalLength.coerceIn(state.focalLengths.first(), state.focalLengths.last())
        if (coerced == state.selectedFocalLength) return
        _uiState.update { it.copy(selectedFocalLength = coerced) }
    }

    fun generateComparison() {
        val state = _uiState.value
        if (!state.isGenerateEnabled) {
            _uiState.update { it.copy(comparisonResults = null) }
            return
        }
        val sensorLookup = state.availableSensors.associateBy { it.value }
        val nativeFocals = state.devices.flatMap { device ->
            device.lenses.mapNotNull { lens ->
                lens.nativeFocalLength.toDoubleOrNull()?.takeIf { it > 0.0 }
            }
        }
        val focalGrid = (DEFAULT_FOCAL_LENGTHS + nativeFocals).distinct().sorted()
        val processedDevices = state.devices.mapIndexedNotNull { index, device ->
            val sanitizedName = device.name.ifBlank { "デバイス ${index + 1}" }
            val rawLenses = device.lenses.mapNotNull { lens ->
                val focal = lens.nativeFocalLength.toDoubleOrNull()
                val fNumber = lens.fNumber.toDoubleOrNull()
                if (focal == null || focal <= 0.0 || fNumber == null || fNumber <= 0.0) return@mapNotNull null
                val sensorSpec = sensorLookup[lens.selectedSensorValue]
                val manualDescriptor = if (lens.usesManualSensor) lens.manualSensorDescriptor else null
                val metrics = calculateNativeSensorMetrics(sensorSpec, manualDescriptor)
                if (metrics.areaSqMm <= 0.0 || metrics.diagonalMm <= 0.0) return@mapNotNull null
                focal to (fNumber to metrics)
            }
            computeProcessedDevice(sanitizedName, device.colorHex, rawLenses, focalGrid)
        }

        if (processedDevices.isEmpty()) {
            _uiState.update { it.copy(comparisonResults = null, focalLengths = DEFAULT_FOCAL_LENGTHS, selectedFocalLength = DEFAULT_FOCAL_LENGTHS.first()) }
            return
        }
        val availableFocalLengths = processedDevices
            .flatMap { device -> device.metricsByFocalLength.map { it.focalLength35mm } }
            .distinct()
            .sorted()

        if (availableFocalLengths.isEmpty()) {
            _uiState.update { it.copy(comparisonResults = null, focalLengths = DEFAULT_FOCAL_LENGTHS, selectedFocalLength = DEFAULT_FOCAL_LENGTHS.first()) }
            return
        }

        val desiredFocal = state.selectedFocalLength
        val nearestFocal = availableFocalLengths.minByOrNull { abs(it - desiredFocal) } ?: availableFocalLengths.first()

        _uiState.update {
            it.copy(
                comparisonResults = ComparisonResults(
                    focalLengths = availableFocalLengths,
                    devices = processedDevices
                ),
                selectedFocalLength = nearestFocal,
                focalLengths = availableFocalLengths
            )
        }
    }

    private fun updateLens(deviceId: Long, lensId: Long, transform: (LensInputState) -> LensInputState) {
        _uiState.update { current ->
            val devices = current.devices.map { device ->
                if (device.id != deviceId) return@map device
                val lenses = device.lenses.map { lens ->
                    if (lens.id == lensId) transform(lens) else lens
                }
                device.copy(lenses = lenses)
            }
            current.copy(
                devices = devices,
                comparisonResults = null,
                presetErrorMessage = null
            )
        }
    }

    private fun startPresetOperation() {
        _uiState.update { it.copy(isPresetProcessing = true, presetErrorMessage = null) }
    }

    private fun finishPresetOperation(transform: (SensorComparisonUiState) -> SensorComparisonUiState = { it }) {
        _uiState.update { state ->
            transform(state).copy(isPresetProcessing = false, presetErrorMessage = null)
        }
    }

    private fun failPresetOperation(error: Throwable?) {
        val message = error?.localizedMessage?.takeIf { it.isNotBlank() } ?: "プリセット処理に失敗しました"
        _uiState.update { it.copy(isPresetProcessing = false, presetErrorMessage = message) }
    }

    private fun postMessage(message: String) {
        _events.tryEmit(SensorComparisonEvent.ShowMessage(message))
    }

    private fun buildDefaultPresetName(deviceName: String?, nowEpochMillis: Long): String {
        val timestamp = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date(nowEpochMillis))
        val base = deviceName?.trim().orEmpty().ifBlank { "デバイス" }
        return "$base $timestamp".take(40)
    }

    private fun createDeviceFromPreset(
        snapshot: PresetSnapshot,
        sensors: List<SensorSpec>,
        deviceId: Long,
        fallbackColor: String
    ): DeviceInputState {
        val sensorLookup = sensors.associateBy { it.value }
        val deviceSnapshot = snapshot.device
        val lenses = deviceSnapshot.lenses
            .take(MAX_LENSES_PER_DEVICE)
            .map { lensSnapshot ->
                val resolved = resolveSensorSelection(
                    rawValue = lensSnapshot.selectedSensorValue,
                    manualDescriptor = lensSnapshot.manualSensorDescriptor,
                    sensorLookup = sensorLookup
                )
                val manualDescriptor = if (resolved.value == MANUAL_INPUT_SENSOR_VALUE) {
                    resolved.manualDescriptor.orEmpty().ifBlank {
                        lensSnapshot.manualSensorDescriptor.trim().ifBlank { lensSnapshot.selectedSensorValue.trim().ifBlank { DEFAULT_MANUAL_SENSOR_VALUE } }
                    }
                } else {
                    ""
                }
                LensInputState(
                    id = nextLensId++,
                    nativeFocalLength = lensSnapshot.nativeFocalLength.ifBlank { "24" },
                    selectedSensorValue = resolved.value,
                    manualSensorDescriptor = manualDescriptor,
                    fNumber = lensSnapshot.fNumber.ifBlank { "1.8" }
                )
            }
        val sanitizedName = deviceSnapshot.name.ifBlank { snapshot.name.ifBlank { "プリセットデバイス" } }
        val sanitizedColor = sanitizeColorHex(deviceSnapshot.colorHex, fallbackColor)
        return DeviceInputState(
            id = deviceId,
            name = sanitizedName,
            colorHex = sanitizedColor,
            lenses = lenses.ifEmpty { listOf(newDefaultLens()) }
        )
    }

    private fun DeviceInputState.toSnapshot(): PresetDeviceSnapshot {
        return PresetDeviceSnapshot(
            name = name.ifBlank { "デバイス" },
            colorHex = sanitizeColorHex(colorHex),
            lenses = lenses.take(MAX_LENSES_PER_DEVICE).map { it.toSnapshot() }
        )
    }

    private fun LensInputState.toSnapshot(): PresetLensSnapshot {
        val sanitizedFocal = nativeFocalLength.trim().ifBlank { "24" }
        val sanitizedFNumber = fNumber.trim().ifBlank { "1.8" }
        val manual = if (usesManualSensor) manualSensorDescriptor.trim().ifBlank { DEFAULT_MANUAL_SENSOR_VALUE } else ""
        return PresetLensSnapshot(
            nativeFocalLength = sanitizedFocal,
            selectedSensorValue = selectedSensorValue,
            manualSensorDescriptor = manual,
            fNumber = sanitizedFNumber
        )
    }

    private fun sanitizeColorHex(raw: String?, fallback: String = DEFAULT_DEVICE_COLORS.first()): String {
        val normalized = raw?.trim()?.uppercase(Locale.US).orEmpty()
        return if (HEX_REGEX.matches(normalized)) normalized else fallback.uppercase(Locale.US)
    }

    private suspend fun persistDevices(devices: List<DeviceInputState>) {
        withContext(Dispatchers.IO) {
            runCatching {
                val serialized = JSONArray().apply {
                    devices.forEach { device ->
                        val lenses = JSONArray().apply {
                            device.lenses.forEach { lens ->
                                put(
                                    JSONObject().apply {
                                        put("id", lens.id)
                                        put("nativeFocalLength", lens.nativeFocalLength)
                                        put("selectedSensorValue", lens.selectedSensorValue)
                                        val manualDescriptor = if (lens.selectedSensorValue == MANUAL_INPUT_SENSOR_VALUE) {
                                            lens.manualSensorDescriptor.ifBlank { DEFAULT_MANUAL_SENSOR_VALUE }
                                        } else {
                                            lens.manualSensorDescriptor
                                        }
                                        put("manualSensorDescriptor", manualDescriptor)
                                        put("fNumber", lens.fNumber)
                                    }
                                )
                            }
                        }
                        put(
                            JSONObject().apply {
                                put("id", device.id)
                                put("name", device.name)
                                put("colorHex", sanitizeColorHex(device.colorHex))
                                put("lenses", lenses)
                            }
                        )
                    }
                }
                val editor = sharedPreferences.edit()
                val committed = if (serialized.length() == 0) {
                    editor.remove(KEY_SAVED_DEVICES).commit()
                } else {
                    editor.putString(KEY_SAVED_DEVICES, serialized.toString()).commit()
                }
                if (!committed) {
                    Log.w(TAG, "Failed to persist devices")
                }
            }.onFailure { error ->
                Log.e(TAG, "Error while persisting devices", error)
            }
        }
    }

    private suspend fun loadPersistedDevices(sensors: List<SensorSpec>): List<DeviceInputState>? {
        val serialized = withContext(Dispatchers.IO) {
            sharedPreferences.getString(KEY_SAVED_DEVICES, null)
        } ?: return null
        if (serialized.isBlank()) return null

        return try {
            val array = JSONArray(serialized)
            val sensorLookup = sensors.associateBy { it.value }
            if (array.length() == 0) {
                nextDeviceId = 1L
                nextLensId = 1L
                emptyList()
            } else {
                val restored = mutableListOf<DeviceInputState>()
                var highestDeviceId = 0L
                var highestLensId = 0L
                for (i in 0 until array.length()) {
                    val deviceObj = array.optJSONObject(i) ?: continue
                    val rawDeviceId = deviceObj.optLong("id", -1L)
                    val deviceId = if (rawDeviceId > 0) rawDeviceId else highestDeviceId + 1
                    highestDeviceId = maxOf(highestDeviceId, deviceId)

                    val name = deviceObj.optString("name").ifBlank { "デバイス $deviceId" }
                    val colorHex = sanitizeColorHex(deviceObj.optString("colorHex"))
                    val lensArray = deviceObj.optJSONArray("lenses")
                    val lenses = mutableListOf<LensInputState>()
                    if (lensArray != null) {
                        for (j in 0 until lensArray.length()) {
                            val lensObj = lensArray.optJSONObject(j) ?: continue
                            val rawLensId = lensObj.optLong("id", -1L)
                            val lensId = if (rawLensId > 0) rawLensId else highestLensId + 1
                            highestLensId = maxOf(highestLensId, lensId)

                            val nativeFocal = lensObj.optString("nativeFocalLength").trim().ifBlank { "24" }
                            val fNumber = lensObj.optString("fNumber").trim().ifBlank { "1.8" }

                            val selection = resolveSensorSelection(
                                rawValue = lensObj.optString("selectedSensorValue").takeIf { it.isNotBlank() } ?: MANUAL_INPUT_SENSOR_VALUE,
                                manualDescriptor = lensObj.optString("manualSensorDescriptor"),
                                sensorLookup = sensorLookup
                            )

                            lenses += LensInputState(
                                id = lensId,
                                nativeFocalLength = nativeFocal,
                                selectedSensorValue = selection.value,
                                manualSensorDescriptor = selection.manualDescriptor ?: "",
                                fNumber = fNumber
                            )
                        }
                    }
                    if (lenses.isEmpty()) {
                        val fallbackLensId = highestLensId + 1
                        highestLensId = fallbackLensId
                        lenses += LensInputState(
                            id = fallbackLensId,
                            nativeFocalLength = "24",
                            selectedSensorValue = MANUAL_INPUT_SENSOR_VALUE,
                            manualSensorDescriptor = DEFAULT_MANUAL_SENSOR_VALUE,
                            fNumber = "1.8"
                        )
                    }
                    restored += DeviceInputState(
                        id = deviceId,
                        name = name,
                        colorHex = colorHex,
                        lenses = lenses.take(MAX_LENSES_PER_DEVICE)
                    )
                }
                if (restored.isEmpty()) {
                    null
                } else {
                    nextDeviceId = highestDeviceId + 1
                    nextLensId = highestLensId + 1
                    restored.take(MAX_DEVICES)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to restore devices from preferences", e)
            null
        }
    }

    private fun newDefaultLens(): LensInputState {
        return LensInputState(
            id = nextLensId++,
            nativeFocalLength = "24",
            selectedSensorValue = MANUAL_INPUT_SENSOR_VALUE,
            manualSensorDescriptor = DEFAULT_MANUAL_SENSOR_VALUE,
            fNumber = "1.8"
        )
    }

    private suspend fun loadBaseSensors(): List<SensorSpec> {
        val resources = getApplication<Application>().resources
        val rawText = withContext(Dispatchers.IO) {
            resources.openRawResource(R.raw.sensor_database).bufferedReader().use { it.readText() }
        }
        return parseSensorCsv(rawText)
    }

    private fun mergeSensors(
        baseSensors: List<SensorSpec>,
        customSensors: List<CustomSensorEntry>
    ): List<SensorSpec> {
        val manual = baseSensors.firstOrNull { it.isManual }
        val baseNonManual = baseSensors.filterNot { it.isManual }
        val customSpecs = customSensors.map { it.toSensorSpec() }.sortedBy { it.name }
        return if (manual != null) listOf(manual) + baseNonManual + customSpecs else baseNonManual + customSpecs
    }

    private fun createDefaultDevices(sensors: List<SensorSpec>): List<DeviceInputState> {
        val presets = listOf(
            DevicePreset(
                name = "デバイス1",
                lenses = listOf(
                    LensPreset(14.0, "1/2.76", 2.2),
                    LensPreset(23.0, "Sony LYT-900 (IMX06A)", 1.63),
                    LensPreset(70.0, "1/2.51", 1.8),
                    LensPreset(100.0, "1/1.4", 2.6)
                )
            ),
            DevicePreset(
                name = "デバイス2",
                lenses = listOf(
                    LensPreset(15.0, "1/2.75", 2.0),
                    LensPreset(23.0, "Sony IMX989", 1.8),
                    LensPreset(70.0, "1/1.56", 2.1),
                    LensPreset(135.0, "1/1.95", 3.2)
                )
            ),
            DevicePreset(
                name = "デバイス3",
                lenses = listOf(
                    LensPreset(14.0, "Sony IMX707", 2.0),
                    LensPreset(35.0, "Sony IMX803", 1.69),
                    LensPreset(85.0, "Samsung GN2 (S5KGN2)", 2.27)
                )
            )
        )

        val sensorByName = sensors.associateBy { it.name }

        return presets.take(MAX_DEVICES).mapIndexed { index, preset ->
            val deviceId = nextDeviceId++
            val lenses = preset.lenses.take(MAX_LENSES_PER_DEVICE).map { lensPreset ->
                val sensor = sensorByName[lensPreset.sensorName]
                val isManual = sensor == null || sensor.isManual
                LensInputState(
                    id = nextLensId++,
                    nativeFocalLength = lensPreset.focalLength.toString(),
                    selectedSensorValue = if (isManual) MANUAL_INPUT_SENSOR_VALUE else sensor!!.value,
                    manualSensorDescriptor = if (isManual) lensPreset.sensorName else "",
                    fNumber = lensPreset.fNumber.toString()
                )
            }
            DeviceInputState(
                id = deviceId,
                name = preset.name,
                colorHex = sanitizeColorHex(DEFAULT_DEVICE_COLORS[index % DEFAULT_DEVICE_COLORS.size]),
                lenses = lenses.ifEmpty { listOf(newDefaultLens()) }
            )
        }
    }

    private data class DevicePreset(
        val name: String,
        val lenses: List<LensPreset>
    )

    private data class LensPreset(
        val focalLength: Double,
        val sensorName: String,
        val fNumber: Double
    )
}

internal fun resolveSensorSelection(
    rawValue: String,
    manualDescriptor: String,
    sensorLookup: Map<String, SensorSpec>
): ResolvedSensorSelection {
    val trimmed = rawValue.trim()
    if (trimmed == MANUAL_INPUT_SENSOR_VALUE) {
        val normalizedManual = manualDescriptor.trim().ifBlank { DEFAULT_MANUAL_SENSOR_VALUE }
        return ResolvedSensorSelection(MANUAL_INPUT_SENSOR_VALUE, normalizedManual)
    }

    sensorLookup[trimmed]?.let { spec ->
        return ResolvedSensorSelection(spec.value, null)
    }

    val matchedByName = sensorLookup.values.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
    if (matchedByName != null) {
        return ResolvedSensorSelection(matchedByName.value, null)
    }

    val fallbackDescriptor = when {
        manualDescriptor.isNotBlank() -> manualDescriptor.trim()
        trimmed.isNotBlank() -> trimmed
        else -> DEFAULT_MANUAL_SENSOR_VALUE
    }
    return ResolvedSensorSelection(MANUAL_INPUT_SENSOR_VALUE, fallbackDescriptor)
}
