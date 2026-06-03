import XCTest
@testable import sumahohikakuku

final class SensorCsvParsingTests: XCTestCase {

    func testParseSensorCsv_prependsManualOptionAndSortsSupportedManufacturers() {
        let raw = [
            "# ignored comment",
            "Other Sensor,1200,1.0,Unknown",
            "Sony IMX989,5000,1.6,No",
            "OmniVision OV50H,5000,1.2,Yes",
            "Sony LYT-900,5000,1.6,Nona Bayer",
            "Samsung GN2 (S5KGN2),5000,1.4,16-IN-1"
        ].joined(separator: "\n")

        let sensors = parseSensorCsv(raw: raw)

        XCTAssertTrue(sensors.first?.isManual == true)
        XCTAssertEqual(sensors.first?.value, MANUAL_INPUT_SENSOR_VALUE)
        XCTAssertEqual(
            sensors.dropFirst().map { $0.name },
            [
                "Sony LYT-900",
                "Sony IMX989",
                "OmniVision OV50H",
                "Samsung GN2 (S5KGN2)",
                "Other Sensor"
            ]
        )
    }

    func testParseSensorCsv_handlesBomQuotedCommasEscapedQuotesAndInvalidRows() {
        let raw = "\u{FEFF}\"Sony \"\"IMX,999\"\"\",5000,1.0,No\n" +
            "Broken Sensor,not-number,1.0,No\n" +
            "Missing Column,1200,1.0"

        let sensors = parseSensorCsv(raw: raw)
        let sensor = tryUnwrap(sensors.first { !$0.isManual })

        XCTAssertEqual(sensor.name, "Sony \"IMX,999\"")
        XCTAssertEqual(sensor.megapixels, 50.0, accuracy: 0.0)
        XCTAssertEqual(sensor.binningType, "None")
    }

    func testParseSensorCsv_interpretsMegapixelColumnAsHundredBasedValue() {
        let sensors = parseSensorCsv(raw: "Test Sensor,50,1.0,No")
        let sensor = tryUnwrap(sensors.first { !$0.isManual })

        XCTAssertEqual(sensor.megapixels, 0.5, accuracy: 0.0)
    }

    func testNormalizeBinning_usesLocaleIndependentRules() {
        XCTAssertEqual(normalizeBinning(raw: "YES"), "Quad Bayer (2x2)")
        XCTAssertEqual(normalizeBinning(raw: "Nona Bayer"), "Nona (3x3)")
        XCTAssertEqual(normalizeBinning(raw: "16-IN-1"), "16-cell (4x4)")
        XCTAssertEqual(normalizeBinning(raw: "Unknown"), "Unknown")
    }

    func testDetectManufacturer_usesLocaleIndependentRules() {
        XCTAssertEqual(detectManufacturer(name: "OMNIVISION OV50"), "OmniVision")
        XCTAssertEqual(detectManufacturer(name: "SMARTSENS SC580"), "SmartSens")
        XCTAssertEqual(detectManufacturer(name: "TOSHIBA T4K37"), "Toshiba")
        XCTAssertEqual(detectManufacturer(name: "Unknown Sensor"), "Other")
    }

    func testParseSensorCsv_skipsBlankCommentsAndMalformedRowsWithoutCreatingEmptySensors() {
        let raw = [
            "",
            "# comment",
            "Good Sensor,1200,1.0,No",
            "Bad Pixel Size,1200,not-number,No",
            "Too,Many,Columns,In,Row"
        ].joined(separator: "\n")

        let sensors = parseSensorCsv(raw: raw)

        XCTAssertEqual(sensors.dropFirst().map { $0.name }, ["Good Sensor"])
        XCTAssertFalse(sensors.dropFirst().contains { $0.name.isEmpty })
    }
}

final class SensorMetricComputationTests: XCTestCase {

    func testIsValidManualSensorDescriptor_acceptsLocalizedFractionFormsAndWhitespace() {
        let validDescriptors = [
            "1/1.33",
            " 1 / 1,33 ",
            "１／１．３３"
        ]

        for descriptor in validDescriptors {
            XCTAssertTrue(
                isValidManualSensorDescriptor(descriptor: descriptor),
                "Expected valid manual descriptor: \(descriptor)"
            )
        }
    }

    func testIsValidManualSensorDescriptor_rejectsBlankMalformedAndZeroFractions() {
        let invalidDescriptors = [
            "",
            "abc/1.28",
            "1//1.28",
            "1/1.28/2",
            "/1.28",
            "1/",
            "0/1.33",
            "1/0"
        ]

        for descriptor in invalidDescriptors {
            XCTAssertFalse(
                isValidManualSensorDescriptor(descriptor: descriptor),
                "Expected invalid manual descriptor: \(descriptor)"
            )
        }
    }

    func testCalculateNativeSensorMetrics_databaseSensorCalculatesFourByThreeGeometry() {
        let spec = SensorSpec(
            name: "Sony IMX999",
            value: "Sony IMX999",
            megapixels: 50.0,
            pixelSizeUm: 1.0,
            binningType: "None",
            manufacturer: "Sony",
            source: .DATABASE
        )

        let metrics = calculateNativeSensorMetrics(sensorSpec: spec, manualDescriptor: "1/1.33")

        XCTAssertEqual(metrics.sensorName, "Sony IMX999")
        XCTAssertEqual(metrics.source, .DATABASE)
        XCTAssertEqual(metrics.areaSqMm, 50.0, accuracy: 0.000000001)
        XCTAssertEqual(metrics.widthMm / metrics.heightMm, 4.0 / 3.0, accuracy: 0.000000001)
        XCTAssertEqual(metrics.nativePixelSizeUm, 1.0, accuracy: 0.0)
    }

    func testCalculateNativeSensorMetrics_invalidDatabaseSensorReturnsZeroMetrics() {
        let spec = SensorSpec(
            name: "Broken Sensor",
            value: "Broken Sensor",
            megapixels: 0.0,
            pixelSizeUm: 1.0,
            binningType: "None",
            manufacturer: "Other",
            source: .DATABASE
        )

        let metrics = calculateNativeSensorMetrics(sensorSpec: spec, manualDescriptor: nil)

        XCTAssertEqual(metrics.diagonalMm, 0.0, accuracy: 0.0)
        XCTAssertEqual(metrics.areaSqMm, 0.0, accuracy: 0.0)
        XCTAssertEqual(metrics.sensorName, "Broken Sensor")
        XCTAssertEqual(metrics.source, .DATABASE)
    }

    func testCalculateNativeSensorMetrics_manualDescriptorUsesOpticalFormatFactor() {
        let metrics = calculateNativeSensorMetrics(sensorSpec: nil, manualDescriptor: "1/1.33")

        let expectedDiagonal = 16.0 / 1.33
        XCTAssertEqual(metrics.diagonalMm, expectedDiagonal, accuracy: 0.000000001)
        XCTAssertEqual(metrics.widthMm, expectedDiagonal * 4.0 / 5.0, accuracy: 0.000000001)
        XCTAssertEqual(metrics.heightMm, expectedDiagonal * 3.0 / 5.0, accuracy: 0.000000001)
        XCTAssertEqual(metrics.areaSqMm, metrics.widthMm * metrics.heightMm, accuracy: 0.000000001)
        XCTAssertEqual(metrics.sensorName, "1/1.33")
        XCTAssertEqual(metrics.source, .MANUAL)
    }

    func testCalculateNativeSensorMetrics_malformedManualDescriptorReturnsZeroWithFailureLabel() {
        let malformed = calculateNativeSensorMetrics(sensorSpec: nil, manualDescriptor: "1/1.28/2")
        let blank = calculateNativeSensorMetrics(sensorSpec: nil, manualDescriptor: "  ")

        XCTAssertEqual(malformed.areaSqMm, 0.0, accuracy: 0.0)
        XCTAssertEqual(malformed.sensorName, "1/1.28/2")
        XCTAssertEqual(blank.areaSqMm, 0.0, accuracy: 0.0)
        XCTAssertEqual(blank.sensorName, "N/A")
    }
}

final class InputSanitizerTests: XCTestCase {

    func testSanitizeDecimalInput_keepsDigitsAndOnlyTheFirstDecimalSeparator() {
        XCTAssertEqual(sanitizeDecimalInput(raw: " １２,３．４abc5 "), "12.345")
    }

    func testSanitizeDecimalInput_returnsEmptyWhenInputHasNoDecimalCharacters() {
        XCTAssertEqual(sanitizeDecimalInput(raw: "abc-+"), "")
    }

    func testSanitizeHexInput_normalizesCasePrefixAndLength() {
        XCTAssertEqual(sanitizeHexInput(raw: "ab-cd#12zz34"), "#ABCD12")
    }

    func testSanitizeHexInput_returnsPrefixWhenNoHexDigitsRemain() {
        XCTAssertEqual(sanitizeHexInput(raw: ""), "#")
        XCTAssertEqual(sanitizeHexInput(raw: "zzzz"), "#")
    }

    func testParseHexColor_acceptsPrefixedAndUnprefixedColors() {
        XCTAssertEqual(parseHexColor(input: "#a1b2c3"), "#A1B2C3")
        XCTAssertEqual(parseHexColor(input: "a1b2c3"), "#A1B2C3")
    }

    func testParseHexColor_rejectsInvalidLengthOrCharacters() {
        XCTAssertNil(parseHexColor(input: "#12G456"))
        XCTAssertNil(parseHexColor(input: "#12345"))
        XCTAssertNil(parseHexColor(input: "#1234567"))
    }
}

final class PreferencesKeyEncodingTests: XCTestCase {

    func testEncodePreferencesKeyComponent_returnsStableLowercaseUtf8Hex() {
        XCTAssertEqual(encodePreferencesKeyComponent("sensor:wide_1"), "73656e736f723a776964655f31")
    }

    func testEncodePreferencesKeyComponent_distinguishesReservedCharacters() {
        XCTAssertNotEqual(
            encodePreferencesKeyComponent("custom:1"),
            encodePreferencesKeyComponent("custom_1")
        )
    }

    func testEncodePreferencesKeyComponent_producesDataStoreSafeKeyComponents() {
        let encoded = encodePreferencesKeyComponent("custom:Sony IMX989/1")

        XCTAssertTrue(encoded.allSatisfy { $0.isNumber || ($0 >= "a" && $0 <= "f") })
    }
}

final class GraphSettingsTests: XCTestCase {

    func testGlowLineWidth_scalesLineWidthByDefaultMultiplier() {
        let settings = GraphSettings(lineWidth: 3.0)

        XCTAssertEqual(settings.glowLineWidth, 7.5, accuracy: 0.0001)
    }

    func testExportAspectRatio_usesConfiguredWidthAndHeight() {
        let settings = GraphSettings(exportAspectWidth: 16, exportAspectHeight: 9)

        XCTAssertEqual(settings.exportAspectRatio, Float(16.0 / 9.0), accuracy: 0.0001)
    }
}

final class CustomSensorModelsTests: XCTestCase {

    func testToSensorSpec_preservesIdAsValueAndNormalizesDerivedFields() {
        let entry = CustomSensorEntry(
            id: "custom:sony-lyt900",
            name: "Sony LYT-900",
            megapixels: 50.0,
            pixelSizeUm: 1.6,
            binningType: "16-IN-1"
        )

        let spec = entry.toSensorSpec()

        XCTAssertEqual(spec.value, "custom:sony-lyt900")
        XCTAssertEqual(spec.manufacturer, "Sony")
        XCTAssertEqual(spec.binningType, "16-cell (4x4)")
        XCTAssertEqual(spec.source, .DATABASE)
        XCTAssertFalse(spec.isManual)
    }
}

final class SensorComparisonUiStateTests: XCTestCase {

    func testIsGenerateEnabled_acceptsAnyCompleteValidLens() {
        let state = SensorComparisonUiState(
            devices: [
                device(id: 1, lenses: [Self.lens(nativeFocalLength: "bad")]),
                device(id: 2, lenses: [Self.lens(selectedSensorValue: "database-sensor")])
            ]
        )

        XCTAssertTrue(state.isGenerateEnabled)
    }

    func testIsGenerateEnabled_rejectsInvalidManualDescriptor() {
        let state = SensorComparisonUiState(
            devices: [
                device(
                    lenses: [
                        Self.lens(
                            selectedSensorValue: MANUAL_INPUT_SENSOR_VALUE,
                            manualSensorDescriptor: "bad descriptor"
                        )
                    ]
                )
            ]
        )

        XCTAssertFalse(state.isGenerateEnabled)
    }

    func testIsGenerateEnabled_rejectsEndFNumberWithoutOpticalEndFocalLength() {
        let state = SensorComparisonUiState(
            devices: [
                device(lenses: [Self.lens(opticalEndFocalLength: "", endFNumber: "2.96")])
            ]
        )

        XCTAssertFalse(state.isGenerateEnabled)
    }

    func testCanAddDevice_reflectsMaximumDeviceLimit() {
        let fourDevices = (1..<MAX_DEVICES).map { device(id: Int64($0)) }
        let fiveDevices = (1...MAX_DEVICES).map { device(id: Int64($0)) }

        XCTAssertTrue(SensorComparisonUiState(devices: fourDevices).canAddDevice)
        XCTAssertFalse(SensorComparisonUiState(devices: fiveDevices).canAddDevice)
    }

    func testPresetListItems_normalizesInvalidColorsAndUsesPresetNameForBlankDeviceName() {
        let state = SensorComparisonUiState(
            presets: [
                PresetSnapshot(
                    id: "preset-1",
                    name: "Travel Kit",
                    device: PresetDeviceSnapshot(
                        name: "",
                        colorHex: "not-a-color",
                        lenses: [
                            PresetLensSnapshot(
                                nativeFocalLength: "24",
                                selectedSensorValue: MANUAL_INPUT_SENSOR_VALUE,
                                manualSensorDescriptor: "1/1.33",
                                fNumber: "1.8"
                            )
                        ]
                    ),
                    updatedAtEpochMillis: 123
                )
            ]
        )

        let item = tryUnwrap(state.presetListItems.first)

        XCTAssertEqual(item.id, "preset-1")
        XCTAssertEqual(item.name, "Travel Kit")
        XCTAssertEqual(item.deviceName, "Travel Kit")
        XCTAssertEqual(item.lensCount, 1)
        XCTAssertEqual(item.colorHex, DEFAULT_DEVICE_COLORS.first)
        XCTAssertEqual(item.updatedAtEpochMillis, 123)
    }

    func testIsPresetSaveEnabled_requiresNameAndTargetDeviceWithLenses() {
        let targetDevice = device(id: 1, lenses: [Self.lens()])

        XCTAssertFalse(
            SensorComparisonUiState(
                devices: [targetDevice],
                presetNameInput: " ",
                presetTargetDeviceId: targetDevice.id
            ).isPresetSaveEnabled
        )
        XCTAssertFalse(
            SensorComparisonUiState(
                devices: [DeviceInputState(id: targetDevice.id, name: "Empty", colorHex: "#2563EB", lenses: [])],
                presetNameInput: "Preset",
                presetTargetDeviceId: targetDevice.id
            ).isPresetSaveEnabled
        )
        XCTAssertTrue(
            SensorComparisonUiState(
                devices: [targetDevice],
                presetNameInput: "Preset",
                presetTargetDeviceId: targetDevice.id
            ).isPresetSaveEnabled
        )
    }

    private func device(
        id: Int64 = 1,
        lenses: [LensInputState]? = nil
    ) -> DeviceInputState {
        DeviceInputState(
            id: id,
            name: "Device \(id)",
            colorHex: DEFAULT_DEVICE_COLORS.first!,
            lenses: lenses ?? [Self.lens()]
        )
    }

    private static func lens(
        id: Int64 = 1,
        nativeFocalLength: String = "24",
        selectedSensorValue: String = MANUAL_INPUT_SENSOR_VALUE,
        manualSensorDescriptor: String = "1/1.33",
        fNumber: String = "1.8",
        opticalEndFocalLength: String = "",
        endFNumber: String = ""
    ) -> LensInputState {
        LensInputState(
            id: id,
            nativeFocalLength: nativeFocalLength,
            selectedSensorValue: selectedSensorValue,
            manualSensorDescriptor: manualSensorDescriptor,
            fNumber: fNumber,
            opticalEndFocalLength: opticalEndFocalLength,
            endFNumber: endFNumber
        )
    }
}

final class ResolveSensorSelectionTests: XCTestCase {

    private let sensor = SensorSpec(
        name: "Sony IMX999",
        value: "sensor:sony-imx999",
        megapixels: 50.0,
        pixelSizeUm: 1.0,
        binningType: "None",
        manufacturer: "Sony",
        source: .DATABASE
    )

    func testResolveSensorSelection_returnsExistingSensorByValueAndClearsManualDescriptor() {
        let resolved = resolveSensorSelection(
            rawValue: sensor.value,
            manualDescriptor: "1/1.33",
            sensorLookup: [sensor.value: sensor]
        )

        XCTAssertEqual(resolved.value, sensor.value)
        XCTAssertNil(resolved.manualDescriptor)
    }

    func testResolveSensorSelection_matchesSavedSensorNameIgnoringCase() {
        let resolved = resolveSensorSelection(
            rawValue: "sony imx999",
            manualDescriptor: "",
            sensorLookup: [sensor.value: sensor]
        )

        XCTAssertEqual(resolved.value, sensor.value)
        XCTAssertNil(resolved.manualDescriptor)
    }

    func testResolveSensorSelection_manualSentinelPreservesProvidedDescriptor() {
        let resolved = resolveSensorSelection(
            rawValue: MANUAL_INPUT_SENSOR_VALUE,
            manualDescriptor: " 1/1.33 ",
            sensorLookup: [:]
        )

        XCTAssertEqual(resolved.value, MANUAL_INPUT_SENSOR_VALUE)
        XCTAssertEqual(resolved.manualDescriptor, "1/1.33")
    }

    func testResolveSensorSelection_missingSavedValuePreservesLegacyLabelAsManualDescriptor() {
        let resolved = resolveSensorSelection(
            rawValue: "Legacy Sensor X",
            manualDescriptor: "",
            sensorLookup: [:]
        )

        XCTAssertEqual(resolved.value, MANUAL_INPUT_SENSOR_VALUE)
        XCTAssertEqual(resolved.manualDescriptor, "Legacy Sensor X")
    }

    func testResolveSensorSelection_blankMissingValueUsesDefaultManualDescriptor() {
        let resolved = resolveSensorSelection(
            rawValue: " ",
            manualDescriptor: " ",
            sensorLookup: [:]
        )

        XCTAssertEqual(resolved.value, MANUAL_INPUT_SENSOR_VALUE)
        XCTAssertEqual(resolved.manualDescriptor, "1/1.33")
    }
}

private func tryUnwrap<T>(
    _ value: T?,
    file: StaticString = #filePath,
    line: UInt = #line
) -> T {
    guard let value else {
        XCTFail("Expected non-nil value", file: file, line: line)
        fatalError("Expected non-nil value")
    }
    return value
}
