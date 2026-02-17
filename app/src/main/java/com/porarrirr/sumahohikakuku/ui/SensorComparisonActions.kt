package com.porarrirr.sumahohikakuku.ui

data class SensorComparisonActions(
    val openPresetSave: () -> Unit,
    val openPresetLibrary: () -> Unit,
    val closePresetSheet: () -> Unit,
    val addDevice: () -> Unit,
    val consumeDeviceFocusRequest: () -> Unit,
    val removeDevice: (Long) -> Unit,
    val updateDeviceName: (Long, String) -> Unit,
    val updateDeviceColor: (Long, String) -> Unit,
    val addLens: (Long) -> Unit,
    val removeLens: (Long, Long) -> Unit,
    val updateLensFocalLength: (Long, Long, String) -> Unit,
    val updateLensSensorSelection: (Long, Long, String) -> Unit,
    val updateLensManualDescriptor: (Long, Long, String) -> Unit,
    val updateLensFNumber: (Long, Long, String) -> Unit,
    val generateComparison: () -> Unit,
    val updateFocalLength: (Double) -> Unit,
    val updatePresetNameInput: (String) -> Unit,
    val updatePresetTargetDevice: (Long?) -> Unit,
    val savePreset: () -> Unit,
    val loadPreset: (String) -> Unit,
    val overwriteTargetDeviceFromPreset: (String) -> Unit,
    val deletePreset: (String) -> Unit,
    val renamePreset: (String, String) -> Unit
)
