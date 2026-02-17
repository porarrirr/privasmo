package com.porarrirr.sumahohikakuku.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.data.CustomSensorRepository
import com.porarrirr.sumahohikakuku.data.DeviceInputRepository
import com.porarrirr.sumahohikakuku.data.PresetRepository
import com.porarrirr.sumahohikakuku.data.SavedDeviceInput
import com.porarrirr.sumahohikakuku.data.SavedLensInput
import com.porarrirr.sumahohikakuku.data.SensorDatabaseRepository
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
import com.porarrirr.sumahohikakuku.model.isValidManualSensorDescriptor
import com.porarrirr.sumahohikakuku.model.toSensorSpec
import com.porarrirr.sumahohikakuku.ui.common.UiText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
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


private const val DEFAULT_MANUAL_SENSOR_VALUE = "1/1.33"
private const val TAG = "SensorComparisonViewModel"

private val DEFAULT_FOCAL_LENGTHS = (14..260).map { it.toDouble() }
private val HEX_REGEX = Regex("^#[0-9A-F]{6}$")

sealed interface SensorComparisonEvent {
    data class ShowMessage(val message: UiText) : SensorComparisonEvent
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
    val presetErrorMessage: UiText? = null,
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

class SensorComparisonViewModel @JvmOverloads constructor(
    application: Application,
    private val presetRepository: PresetRepository = PresetRepository(application),
    private val customSensorRepository: CustomSensorRepository = CustomSensorRepository(application),
    private val deviceInputRepository: DeviceInputRepository = DeviceInputRepository(application),
    private val sensorDatabaseRepository: SensorDatabaseRepository = SensorDatabaseRepository(application),
    private val generateComparisonUseCase: GenerateComparisonUseCase = GenerateComparisonUseCase()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SensorComparisonUiState())
    val uiState: StateFlow<SensorComparisonUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SensorComparisonEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SensorComparisonEvent> = _events.asSharedFlow()

    private var nextDeviceId = 1L
    private var nextLensId = 1L
    private var hasRestoredState = false
    private var hasShownDeviceSaveError = false
    private var hasShownCustomSensorLoadError = false
    private var hasShownPresetLoadError = false

    init {
        viewModelScope.launch {
            val baseSensors = sensorDatabaseRepository.loadSensors()
                .getOrElse { error ->
                    Log.e(TAG, "Failed to load sensor database", error)
                    postMessage(UiText.StringResource(R.string.error_failed_to_load_sensor_database))
                    emptyList()
                }
            customSensorRepository.sensorsFlow
                .map { custom -> mergeSensors(baseSensors, custom) }
                .distinctUntilChanged()
                .collectLatest { sensors ->
                    if (!hasRestoredState) {
                        val restoredDevices = deviceInputRepository.load()
                            .onFailure { error ->
                                Log.w(TAG, "Failed to restore devices", error)
                                postMessage(UiText.StringResource(R.string.error_failed_to_restore_device_inputs))
                            }
                            .getOrNull()
                            ?.let { saved -> buildDevicesFromSaved(saved, sensors) }
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

        viewModelScope.launch {
            customSensorRepository.errors.collectLatest {
                if (hasShownCustomSensorLoadError) return@collectLatest
                hasShownCustomSensorLoadError = true
                postMessage(UiText.StringResource(R.string.error_failed_to_load_custom_sensors))
            }
        }

        viewModelScope.launch {
            presetRepository.errors.collectLatest {
                if (hasShownPresetLoadError) return@collectLatest
                hasShownPresetLoadError = true
                postMessage(UiText.StringResource(R.string.error_failed_to_load_presets))
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
            _uiState.update { it.copy(presetErrorMessage = UiText.StringResource(R.string.error_preset_name_required)) }
            return
        }
        val targetDevice = state.presetTargetDevice
        if (targetDevice == null) {
            _uiState.update { it.copy(presetErrorMessage = UiText.StringResource(R.string.error_preset_target_required)) }
            return
        }
        val deviceSnapshot = targetDevice.toSnapshot()
        if (deviceSnapshot.lenses.isEmpty()) {
            _uiState.update { it.copy(presetErrorMessage = UiText.StringResource(R.string.error_preset_no_lenses)) }
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
                    postMessage(
                        UiText.StringResource(
                            R.string.message_preset_saved,
                            listOf(snapshot.name)
                        )
                    )
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
            _uiState.update {
                it.copy(
                    presetErrorMessage = UiText.StringResource(
                        R.string.error_max_devices_reached,
                        listOf(MAX_DEVICES)
                    )
                )
            }
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
        postMessage(UiText.StringResource(R.string.message_device_added, listOf(newDevice.name)))
    }

    fun overwriteTargetDeviceFromPreset(presetId: String) {
        val state = _uiState.value
        val targetDeviceId = state.presetTargetDeviceId
        if (targetDeviceId == null) {
            _uiState.update { it.copy(presetErrorMessage = UiText.StringResource(R.string.error_overwrite_target_required)) }
            return
        }
        val snapshot = state.presets.firstOrNull { it.id == presetId } ?: return
        val targetDevice = state.devices.firstOrNull { it.id == targetDeviceId }
        if (targetDevice == null) {
            _uiState.update { it.copy(presetErrorMessage = UiText.StringResource(R.string.error_overwrite_target_not_found)) }
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
        postMessage(UiText.StringResource(R.string.message_device_overwritten, listOf(overwritten.name)))
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
                    postMessage(UiText.StringResource(R.string.message_preset_deleted))
                }
                .onFailure { error -> failPresetOperation(error) }
        }
    }

    fun renamePreset(presetId: String, newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(presetErrorMessage = UiText.StringResource(R.string.error_new_preset_name_required)) }
            return
        }
        startPresetOperation()
        viewModelScope.launch {
            runCatching { presetRepository.updatePresetName(presetId, trimmedName.take(40)) }
                .onSuccess {
                    finishPresetOperation()
                    postMessage(UiText.StringResource(R.string.message_preset_renamed))
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

        val output = generateComparisonUseCase.generate(
            devices = state.devices,
            availableSensors = state.availableSensors,
            selectedFocalLength = state.selectedFocalLength,
            defaultFocalLengths = DEFAULT_FOCAL_LENGTHS
        )

        _uiState.update {
            it.copy(
                comparisonResults = output.results,
                selectedFocalLength = output.selectedFocalLength,
                focalLengths = output.focalLengths
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
        val message = error?.localizedMessage?.takeIf { it.isNotBlank() }?.let(UiText::Dynamic)
            ?: UiText.StringResource(R.string.error_preset_operation_failed)
        _uiState.update { it.copy(isPresetProcessing = false, presetErrorMessage = message) }
    }

    private fun postMessage(message: UiText) {
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
        val snapshot = devices.map { device ->
            SavedDeviceInput(
                name = device.name,
                colorHex = sanitizeColorHex(device.colorHex),
                lenses = device.lenses.map { lens ->
                    val manualDescriptor = if (lens.selectedSensorValue == MANUAL_INPUT_SENSOR_VALUE) {
                        lens.manualSensorDescriptor.ifBlank { DEFAULT_MANUAL_SENSOR_VALUE }
                    } else {
                        lens.manualSensorDescriptor
                    }
                    SavedLensInput(
                        nativeFocalLength = lens.nativeFocalLength,
                        selectedSensorValue = lens.selectedSensorValue,
                        manualSensorDescriptor = manualDescriptor,
                        fNumber = lens.fNumber
                    )
                }
            )
        }
        deviceInputRepository.save(snapshot)
            .onSuccess { hasShownDeviceSaveError = false }
            .onFailure { error ->
                Log.e(TAG, "Failed to persist devices", error)
                if (!hasShownDeviceSaveError) {
                    hasShownDeviceSaveError = true
                    postMessage(
                        UiText.StringResource(R.string.error_failed_to_save_device_inputs)
                    )
                }
            }
    }

    private fun buildDevicesFromSaved(
        saved: List<SavedDeviceInput>,
        sensors: List<SensorSpec>
    ): List<DeviceInputState> {
        nextDeviceId = 1L
        nextLensId = 1L
        val sensorLookup = sensors.associateBy { it.value }

        return saved
            .take(MAX_DEVICES)
            .mapIndexed { deviceIndex, device ->
                val deviceId = nextDeviceId++
                val fallbackColor = DEFAULT_DEVICE_COLORS[deviceIndex % DEFAULT_DEVICE_COLORS.size]
                val lenses = device.lenses
                    .take(MAX_LENSES_PER_DEVICE)
                    .map { lens ->
                        val selection = resolveSensorSelection(
                            rawValue = lens.selectedSensorValue.takeIf { it.isNotBlank() }
                                ?: MANUAL_INPUT_SENSOR_VALUE,
                            manualDescriptor = lens.manualSensorDescriptor,
                            sensorLookup = sensorLookup
                        )
                        LensInputState(
                            id = nextLensId++,
                            nativeFocalLength = lens.nativeFocalLength.trim().ifBlank { "24" },
                            selectedSensorValue = selection.value,
                            manualSensorDescriptor = selection.manualDescriptor ?: "",
                            fNumber = lens.fNumber.trim().ifBlank { "1.8" }
                        )
                    }
                DeviceInputState(
                    id = deviceId,
                    name = device.name.ifBlank { "デバイス ${deviceIndex + 1}" },
                    colorHex = sanitizeColorHex(device.colorHex, fallbackColor),
                    lenses = lenses.ifEmpty { listOf(newDefaultLens()) }
                )
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
