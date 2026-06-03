import Foundation

private func NSLocalizedString(_ key: String, comment: String) -> String {
    if let languageOverride = UserDefaults.standard.string(forKey: "app_language_override"),
       let path = Bundle.main.path(forResource: languageOverride, ofType: "lproj"),
       let bundle = Bundle(path: path) {
        return bundle.localizedString(forKey: key, value: nil, table: nil)
    }
    return Foundation.NSLocalizedString(key, comment: comment)
}

public struct LocalizedStrings {
    public static var appName: String { NSLocalizedString("appName", comment: "") }
    public static var errorFailedToLoadSensorDatabase: String { NSLocalizedString("errorFailedToLoadSensorDatabase", comment: "") }
    public static var errorFailedToRestoreDeviceInputs: String { NSLocalizedString("errorFailedToRestoreDeviceInputs", comment: "") }
    public static var errorFailedToSaveDeviceInputs: String { NSLocalizedString("errorFailedToSaveDeviceInputs", comment: "") }
    public static var errorFailedToLoadCustomSensors: String { NSLocalizedString("errorFailedToLoadCustomSensors", comment: "") }
    public static var errorFailedToSaveCustomSensors: String { NSLocalizedString("errorFailedToSaveCustomSensors", comment: "") }
    public static var errorFailedToLoadPresets: String { NSLocalizedString("errorFailedToLoadPresets", comment: "") }
    public static var errorPresetNameRequired: String { NSLocalizedString("errorPresetNameRequired", comment: "") }
    public static var errorPresetTargetRequired: String { NSLocalizedString("errorPresetTargetRequired", comment: "") }
    public static var errorPresetNoLenses: String { NSLocalizedString("errorPresetNoLenses", comment: "") }
    public static var errorMaxDevicesReached: String { NSLocalizedString("errorMaxDevicesReached", comment: "") }
    public static var errorOverwriteTargetRequired: String { NSLocalizedString("errorOverwriteTargetRequired", comment: "") }
    public static var errorOverwriteTargetNotFound: String { NSLocalizedString("errorOverwriteTargetNotFound", comment: "") }
    public static var errorNewPresetNameRequired: String { NSLocalizedString("errorNewPresetNameRequired", comment: "") }
    public static var errorPresetOperationFailed: String { NSLocalizedString("errorPresetOperationFailed", comment: "") }

    public static var sensorComparisonTitle: String { NSLocalizedString("sensorComparisonTitle", comment: "") }
    public static var actionSave: String { NSLocalizedString("actionSave", comment: "") }
    public static var actionLoad: String { NSLocalizedString("actionLoad", comment: "") }
    public static var actionList: String { NSLocalizedString("actionList", comment: "") }
    public static var actionClose: String { NSLocalizedString("actionClose", comment: "") }
    public static var actionClear: String { NSLocalizedString("actionClear", comment: "") }
    public static var actionCancel: String { NSLocalizedString("actionCancel", comment: "") }
    public static var actionRename: String { NSLocalizedString("actionRename", comment: "") }
    public static var actionAddDevice: String { NSLocalizedString("actionAddDevice", comment: "") }
    public static var actionAddDeviceAsDevice: String { NSLocalizedString("actionAddDeviceAsDevice", comment: "") }
    public static var actionOverwrite: String { NSLocalizedString("actionOverwrite", comment: "") }
    public static var actionOpenColorPalette: String { NSLocalizedString("actionOpenColorPalette", comment: "") }
    public static var actionSaveResultsImage: String { NSLocalizedString("actionSaveResultsImage", comment: "") }
    public static var actionShareResultsImage: String { NSLocalizedString("actionShareResultsImage", comment: "") }
    public static var actionBack: String { NSLocalizedString("actionBack", comment: "") }
    public static var actionAdd: String { NSLocalizedString("actionAdd", comment: "") }
    public static var actionEdit: String { NSLocalizedString("actionEdit", comment: "") }
    public static var actionDelete: String { NSLocalizedString("actionDelete", comment: "") }
    public static var actionResetDefault: String { NSLocalizedString("actionResetDefault", comment: "") }
    public static var actionRegenerate: String { NSLocalizedString("actionRegenerate", comment: "") }

    public static var menuSettings: String { NSLocalizedString("menuSettings", comment: "") }
    public static var settingsTitle: String { NSLocalizedString("settingsTitle", comment: "") }

    public static var buttonGenerateGraph: String { NSLocalizedString("buttonGenerateGraph", comment: "") }
    public static var buttonAddLensWithMax: String { NSLocalizedString("buttonAddLensWithMax", comment: "") }
    public static var buttonSaveAsPreset: String { NSLocalizedString("buttonSaveAsPreset", comment: "") }

    public static var labelName: String { NSLocalizedString("labelName", comment: "") }
    public static var labelSensorName: String { NSLocalizedString("labelSensorName", comment: "") }
    public static var labelTargetDevice: String { NSLocalizedString("labelTargetDevice", comment: "") }
    public static var labelPresetSaveTitle: String { NSLocalizedString("labelPresetSaveTitle", comment: "") }
    public static var labelPresetSaveTargetDevice: String { NSLocalizedString("labelPresetSaveTargetDevice", comment: "") }
    public static var labelPresetSaveTargetDeviceField: String { NSLocalizedString("labelPresetSaveTargetDeviceField", comment: "") }
    public static var labelPresetNewName: String { NSLocalizedString("labelPresetNewName", comment: "") }
    public static var labelPresetOverwriteTargetDevice: String { NSLocalizedString("labelPresetOverwriteTargetDevice", comment: "") }
    public static var labelPresetName: String { NSLocalizedString("labelPresetName", comment: "") }
    public static var labelFocalLength: String { NSLocalizedString("labelFocalLength", comment: "") }
    public static var labelFNumber: String { NSLocalizedString("labelFNumber", comment: "") }
    public static var labelOpticalEndFocalLength: String { NSLocalizedString("labelOpticalEndFocalLength", comment: "") }
    public static var labelEndFNumber: String { NSLocalizedString("labelEndFNumber", comment: "") }
    public static var labelSensor: String { NSLocalizedString("labelSensor", comment: "") }
    public static var labelMegapixels: String { NSLocalizedString("labelMegapixels", comment: "") }
    public static var labelPixelSizeUm: String { NSLocalizedString("labelPixelSizeUm", comment: "") }
    public static var labelBinning: String { NSLocalizedString("labelBinning", comment: "") }
    public static var labelChartLineWidth: String { NSLocalizedString("labelChartLineWidth", comment: "") }
    public static var labelExportImageAspectRatio: String { NSLocalizedString("labelExportImageAspectRatio", comment: "") }
    public static var helperExportImageAspectRatioRange: String { NSLocalizedString("helperExportImageAspectRatioRange", comment: "") }
    public static var labelCurrentExportAspectRatio: String { NSLocalizedString("labelCurrentExportAspectRatio", comment: "") }
    public static var labelAspectRatioWidth: String { NSLocalizedString("labelAspectRatioWidth", comment: "") }
    public static var labelAspectRatioHeight: String { NSLocalizedString("labelAspectRatioHeight", comment: "") }
    public static var actionApplyExportImageAspectRatio: String { NSLocalizedString("actionApplyExportImageAspectRatio", comment: "") }
    public static var labelCustomSensorList: String { NSLocalizedString("labelCustomSensorList", comment: "") }
    public static var labelManualInputExample: String { NSLocalizedString("labelManualInputExample", comment: "") }
    public static var helperInvalidFractionFormat: String { NSLocalizedString("helperInvalidFractionFormat", comment: "") }
    public static var labelSearch: String { NSLocalizedString("labelSearch", comment: "") }
    public static var placeholderSearchSensor: String { NSLocalizedString("placeholderSearchSensor", comment: "") }
    public static var labelAll: String { NSLocalizedString("labelAll", comment: "") }
    public static var labelCustomColor: String { NSLocalizedString("labelCustomColor", comment: "") }
    public static var helperHexSixDigits: String { NSLocalizedString("helperHexSixDigits", comment: "") }
    public static var titleColorPalette: String { NSLocalizedString("titleColorPalette", comment: "") }

    public static var dialogTitleOverwrite: String { NSLocalizedString("dialogTitleOverwrite", comment: "") }
    public static var dialogConfirmOverwrite: String { NSLocalizedString("dialogConfirmOverwrite", comment: "") }
    public static var dialogDeleteSensorTitle: String { NSLocalizedString("dialogDeleteSensorTitle", comment: "") }
    public static var dialogDeleteSensorMessage: String { NSLocalizedString("dialogDeleteSensorMessage", comment: "") }
    public static var dialogDeleteDeviceTitle: String { NSLocalizedString("dialogDeleteDeviceTitle", comment: "") }
    public static var dialogDeleteDeviceMessage: String { NSLocalizedString("dialogDeleteDeviceMessage", comment: "") }

    public static var toastImageSaved: String { NSLocalizedString("toastImageSaved", comment: "") }
    public static var errorFailedToSaveImage: String { NSLocalizedString("errorFailedToSaveImage", comment: "") }
    public static var errorFailedToCreateShareImage: String { NSLocalizedString("errorFailedToCreateShareImage", comment: "") }
    public static var messagePresetSaved: String { NSLocalizedString("messagePresetSaved", comment: "") }
    public static var messageDeviceAdded: String { NSLocalizedString("messageDeviceAdded", comment: "") }
    public static var messageDeviceOverwritten: String { NSLocalizedString("messageDeviceOverwritten", comment: "") }
    public static var messagePresetDeleted: String { NSLocalizedString("messagePresetDeleted", comment: "") }
    public static var messagePresetRenamed: String { NSLocalizedString("messagePresetRenamed", comment: "") }
    public static var titleAddSensor: String { NSLocalizedString("titleAddSensor", comment: "") }
    public static var titleEditSensor: String { NSLocalizedString("titleEditSensor", comment: "") }
    public static var errorDuplicateSensorName: String { NSLocalizedString("errorDuplicateSensorName", comment: "") }
    public static var helperCustomSensorEditor: String { NSLocalizedString("helperCustomSensorEditor", comment: "") }
    public static var textNoCustomSensors: String { NSLocalizedString("textNoCustomSensors", comment: "") }

    public static var labelLens: String { NSLocalizedString("labelLens", comment: "") }
    public static var labelColor: String { NSLocalizedString("labelColor", comment: "") }
    public static var tabDeviceInput: String { NSLocalizedString("tabDeviceInput", comment: "") }
    public static var tabComparisonGraph: String { NSLocalizedString("tabComparisonGraph", comment: "") }
    public static var messageGenerateGraphFirst: String { NSLocalizedString("messageGenerateGraphFirst", comment: "") }
    public static var sectionComparisonGraph: String { NSLocalizedString("sectionComparisonGraph", comment: "") }
    public static var sectionInteractive: String { NSLocalizedString("sectionInteractive", comment: "") }
    public static var labelDimensionsWidthHeight: String { NSLocalizedString("labelDimensionsWidthHeight", comment: "") }
    public static var chartMarkerDefaultLabel: String { NSLocalizedString("chartMarkerDefaultLabel", comment: "") }
    public static var chartMarkerFocalLengthMm: String { NSLocalizedString("chartMarkerFocalLengthMm", comment: "") }
    public static var chartMarkerZoomDigital: String { NSLocalizedString("chartMarkerZoomDigital", comment: "") }
    public static var chartMarkerZoomOptical: String { NSLocalizedString("chartMarkerZoomOptical", comment: "") }
    public static var chartMarkerValueFormat: String { NSLocalizedString("chartMarkerValueFormat", comment: "") }
    public static var chartMarkerUnitPrefix: String { NSLocalizedString("chartMarkerUnitPrefix", comment: "") }

    public static var appTitleAdvanced: String { NSLocalizedString("appTitleAdvanced", comment: "") }
    public static var descriptionManualInputGuidance: String { NSLocalizedString("descriptionManualInputGuidance", comment: "") }
    public static var sectionDevices: String { NSLocalizedString("sectionDevices", comment: "") }
    public static var descriptionDevices: String { NSLocalizedString("descriptionDevices", comment: "") }
    public static var titlePresets: String { NSLocalizedString("titlePresets", comment: "") }
    public static var descriptionPresetSummary: String { NSLocalizedString("descriptionPresetSummary", comment: "") }
    public static var labelDeviceDefaultName: String { NSLocalizedString("labelDeviceDefaultName", comment: "") }
    public static var labelDeviceNumberedName: String { NSLocalizedString("labelDeviceNumberedName", comment: "") }
    public static var labelPresetDeviceDefaultName: String { NSLocalizedString("labelPresetDeviceDefaultName", comment: "") }
    public static var labelUntitledDevice: String { NSLocalizedString("labelUntitledDevice", comment: "") }
    public static var labelUnselected: String { NSLocalizedString("labelUnselected", comment: "") }
    public static var labelNone: String { NSLocalizedString("labelNone", comment: "") }
    public static var textSourcePresetForDevice: String { NSLocalizedString("textSourcePresetForDevice", comment: "") }
    public static var textSavedCount: String { NSLocalizedString("textSavedCount", comment: "") }
    public static var textNoOperableDevices: String { NSLocalizedString("textNoOperableDevices", comment: "") }
    public static var descriptionSelectedDeviceSaved: String { NSLocalizedString("descriptionSelectedDeviceSaved", comment: "") }
    public static var titlePresetLibrary: String { NSLocalizedString("titlePresetLibrary", comment: "") }
    public static var textNoOverwriteTargetDevices: String { NSLocalizedString("textNoOverwriteTargetDevices", comment: "") }
    public static var descriptionPresetAddOrOverwrite: String { NSLocalizedString("descriptionPresetAddOrOverwrite", comment: "") }
    public static var textMaxDevicesReachedHint: String { NSLocalizedString("textMaxDevicesReachedHint", comment: "") }
    public static var titleSavedPresetsWithCount: String { NSLocalizedString("titleSavedPresetsWithCount", comment: "") }
    public static var textNoPresetsRegistered: String { NSLocalizedString("textNoPresetsRegistered", comment: "") }
    public static var labelLastUpdatedUnknown: String { NSLocalizedString("labelLastUpdatedUnknown", comment: "") }
    public static var labelLastUpdated: String { NSLocalizedString("labelLastUpdated", comment: "") }
    public static var labelDeviceNameWithValue: String { NSLocalizedString("labelDeviceNameWithValue", comment: "") }
    public static var labelLensCountWithValue: String { NSLocalizedString("labelLensCountWithValue", comment: "") }
    public static var labelSourceForTarget: String { NSLocalizedString("labelSourceForTarget", comment: "") }
    public static var dialogOverwriteTargetMessage: String { NSLocalizedString("dialogOverwriteTargetMessage", comment: "") }
    public static var contentDescDeletePreset: String { NSLocalizedString("contentDescDeletePreset", comment: "") }
    public static var contentDescDeleteDevice: String { NSLocalizedString("contentDescDeleteDevice", comment: "") }
    public static var contentDescDeleteLens: String { NSLocalizedString("contentDescDeleteLens", comment: "") }
    public static var hintSwipeDevices: String { NSLocalizedString("hintSwipeDevices", comment: "") }
    public static var labelManualInput: String { NSLocalizedString("labelManualInput", comment: "") }
    public static var titleSensorPicker: String { NSLocalizedString("titleSensorPicker", comment: "") }
    public static var textNoMatchingSensors: String { NSLocalizedString("textNoMatchingSensors", comment: "") }
    public static var labelSelectedShort: String { NSLocalizedString("labelSelectedShort", comment: "") }
    public static var labelGraphDisplayRange: String { NSLocalizedString("labelGraphDisplayRange", comment: "") }
    public static var labelGraphRangeCompact: String { NSLocalizedString("labelGraphRangeCompact", comment: "") }
    public static var labelLeftEdge: String { NSLocalizedString("labelLeftEdge", comment: "") }
    public static var labelRightEdge: String { NSLocalizedString("labelRightEdge", comment: "") }
    public static var labelSelectedFocalLength: String { NSLocalizedString("labelSelectedFocalLength", comment: "") }
    public static var chartTitleEffectiveArea: String { NSLocalizedString("chartTitleEffectiveArea", comment: "") }
    public static var chartYLabelEffectiveArea: String { NSLocalizedString("chartYLabelEffectiveArea", comment: "") }
    public static var chartTitleLightIntake: String { NSLocalizedString("chartTitleLightIntake", comment: "") }
    public static var chartYLabelLightIntake: String { NSLocalizedString("chartYLabelLightIntake", comment: "") }
    public static var exportTitle: String { NSLocalizedString("exportTitle", comment: "") }
    public static var exportLabelCreatedAt: String { NSLocalizedString("exportLabelCreatedAt", comment: "") }
    public static var exportLabelRange: String { NSLocalizedString("exportLabelRange", comment: "") }
    public static var exportSectionChart: String { NSLocalizedString("exportSectionChart", comment: "") }
    public static var exportSectionSpecsAt: String { NSLocalizedString("exportSectionSpecsAt", comment: "") }
    public static var labelPixelPitch: String { NSLocalizedString("labelPixelPitch", comment: "") }
    public static var labelPixelPitchBinned: String { NSLocalizedString("labelPixelPitchBinned", comment: "") }
    public static var labelNotAvailable: String { NSLocalizedString("labelNotAvailable", comment: "") }
    public static var metricEffectiveArea: String { NSLocalizedString("metricEffectiveArea", comment: "") }
    public static var metricTotalLightIntake: String { NSLocalizedString("metricTotalLightIntake", comment: "") }
    public static var metricActualFocalLength: String { NSLocalizedString("metricActualFocalLength", comment: "") }
    public static var metricFNumber: String { NSLocalizedString("metricFNumber", comment: "") }
    public static var metricSensorUsed: String { NSLocalizedString("metricSensorUsed", comment: "") }
    public static var metricEffectiveDimensions: String { NSLocalizedString("metricEffectiveDimensions", comment: "") }
    public static var metricEffectiveAperture: String { NSLocalizedString("metricEffectiveAperture", comment: "") }
    public static var metricDigitalZoom: String { NSLocalizedString("metricDigitalZoom", comment: "") }
    public static var labelVariableOpticalRange: String { NSLocalizedString("labelVariableOpticalRange", comment: "") }
    public static var labelDigitalCropRatio: String { NSLocalizedString("labelDigitalCropRatio", comment: "") }
    public static var labelOpticalZoomRatio: String { NSLocalizedString("labelOpticalZoomRatio", comment: "") }
    public static var labelEstimatedFNumber: String { NSLocalizedString("labelEstimatedFNumber", comment: "") }
    public static var metricLensUsed: String { NSLocalizedString("metricLensUsed", comment: "") }
    public static var valueLensUsedFormat: String { NSLocalizedString("valueLensUsedFormat", comment: "") }
    public static var metricBinningCharacteristic: String { NSLocalizedString("metricBinningCharacteristic", comment: "") }
    public static var metricNativePixelPitch: String { NSLocalizedString("metricNativePixelPitch", comment: "") }
    public static var metricEffectivePixelPitch: String { NSLocalizedString("metricEffectivePixelPitch", comment: "") }
    public static var metricApertureArea: String { NSLocalizedString("metricApertureArea", comment: "") }
    public static var actionHideDetails: String { NSLocalizedString("actionHideDetails", comment: "") }
    public static var actionShowDetails: String { NSLocalizedString("actionShowDetails", comment: "") }
    public static var errorActivityNotFound: String { NSLocalizedString("errorActivityNotFound", comment: "") }
    public static var titleShareImage: String { NSLocalizedString("titleShareImage", comment: "") }

    /* New Keys */
    public static var errorNameRequired: String { NSLocalizedString("errorNameRequired", comment: "") }
    public static var errorMegapixelsInvalid: String { NSLocalizedString("errorMegapixelsInvalid", comment: "") }
    public static var errorPixelSizeInvalid: String { NSLocalizedString("errorPixelSizeInvalid", comment: "") }
    public static var actionShareAreaChart: String { NSLocalizedString("actionShareAreaChart", comment: "") }
    public static var actionShareLightIntakeChart: String { NSLocalizedString("actionShareLightIntakeChart", comment: "") }
    public static var labelLensCountCompact: String { NSLocalizedString("labelLensCountCompact", comment: "") }
    public static var dialogRenamePresetTitle: String { NSLocalizedString("dialogRenamePresetTitle", comment: "") }
    public static var labelLanguage: String { NSLocalizedString("labelLanguage", comment: "") }
    public static var labelLanguageAuto: String { NSLocalizedString("labelLanguageAuto", comment: "") }
}
