import XCTest
@testable import sumahohikakuku

final class EffectiveLensMetricsTests: XCTestCase {

    private let wideSensorMetrics = SensorMetrics(
        diagonalMm: 10.0,
        widthMm: 8.0,
        heightMm: 6.0,
        areaSqMm: 48.0,
        sensorName: "Wide Sensor",
        binningType: "None",
        nativePixelSizeUm: 1.0,
        source: .DATABASE
    )

    private let teleSensorMetrics = SensorMetrics(
        diagonalMm: 8.0,
        widthMm: 6.4,
        heightMm: 4.8,
        areaSqMm: 30.72,
        sensorName: "Tele Sensor",
        binningType: "None",
        nativePixelSizeUm: 1.0,
        source: .DATABASE
    )

    private var wideLens: LensProcessed {
        LensProcessed(
            nativeFocalLength35mm: 24.0,
            fNumber: 2.0,
            actualFocalLengthMm: 5.5,
            sensorMetrics: wideSensorMetrics
        )
    }

    private var teleLens: LensProcessed {
        LensProcessed(
            nativeFocalLength35mm: 70.0,
            fNumber: 2.8,
            actualFocalLengthMm: 12.9,
            sensorMetrics: teleSensorMetrics
        )
    }

    func testCalculateEffectiveMetrics_returnsNilWhenNoLensesAreProvided() {
        XCTAssertNil(calculateEffectiveMetrics(focalLength35mm: 24.0, lenses: []))
    }

    func testCalculateEffectiveMetrics_returnsNilBelowFirstReachableFocalLength() {
        let metrics = calculateEffectiveMetrics(focalLength35mm: 13.0, lenses: [wideLens, teleLens])

        XCTAssertNil(metrics)
    }

    func testCalculateEffectiveMetrics_appliesInverseSquareCropAfterNativeFocal() {
        let metrics = tryUnwrap(calculateEffectiveMetrics(focalLength35mm: 48.0, lenses: [wideLens, teleLens]))

        XCTAssertEqual(metrics.baseLens.nativeFocalLength35mm, 24.0, accuracy: 0.0)
        XCTAssertEqual(metrics.opticalFocalLength35mm, 24.0, accuracy: 0.0)
        XCTAssertEqual(metrics.digitalCropRatio, 2.0, accuracy: 0.000000001)
        XCTAssertEqual(metrics.effectiveWidthMm, 4.0, accuracy: 0.000000001)
        XCTAssertEqual(metrics.effectiveHeightMm, 3.0, accuracy: 0.000000001)
        XCTAssertEqual(metrics.effectiveAreaSqMm, 12.0, accuracy: 0.000000001)
        XCTAssertEqual(metrics.totalLightIntake, 3.0, accuracy: 0.000000001)
    }

    func testCalculateEffectiveMetrics_prefersNativeLensAtSharedOpticalBoundary() {
        let variableWideLens = LensProcessed(
            nativeFocalLength35mm: wideLens.nativeFocalLength35mm,
            fNumber: wideLens.fNumber,
            actualFocalLengthMm: wideLens.actualFocalLengthMm,
            sensorMetrics: wideLens.sensorMetrics,
            opticalEndFocalLength35mm: 70.0,
            endFNumber: 2.8
        )

        let beforeBoundary = tryUnwrap(
            calculateEffectiveMetrics(focalLength35mm: 69.0, lenses: [variableWideLens, teleLens])
        )
        let atBoundary = tryUnwrap(
            calculateEffectiveMetrics(focalLength35mm: 70.0, lenses: [variableWideLens, teleLens])
        )

        XCTAssertEqual(beforeBoundary.baseLens.nativeFocalLength35mm, 24.0, accuracy: 0.0)
        XCTAssertEqual(atBoundary.baseLens.nativeFocalLength35mm, 70.0, accuracy: 0.0)
    }

    func testCalculateEffectiveMetrics_keepsFullAreaThroughVariableOpticalRange() {
        let variableTeleLens = LensProcessed(
            nativeFocalLength35mm: 75.0,
            fNumber: 2.39,
            actualFocalLengthMm: 20.0,
            sensorMetrics: wideSensorMetrics,
            opticalEndFocalLength35mm: 100.0,
            endFNumber: 2.96
        )

        let metrics75 = tryUnwrap(calculateEffectiveMetrics(focalLength35mm: 75.0, lenses: [variableTeleLens]))
        let metrics90 = tryUnwrap(calculateEffectiveMetrics(focalLength35mm: 90.0, lenses: [variableTeleLens]))
        let metrics100 = tryUnwrap(calculateEffectiveMetrics(focalLength35mm: 100.0, lenses: [variableTeleLens]))
        let expectedFNumber90 = 2.39 + (2.96 - 2.39) * ((90.0 - 75.0) / (100.0 - 75.0))

        XCTAssertTrue(variableTeleLens.isVariableOptical)
        XCTAssertEqual(metrics75.effectiveAreaSqMm, metrics90.effectiveAreaSqMm, accuracy: 0.000000001)
        XCTAssertEqual(metrics75.effectiveAreaSqMm, metrics100.effectiveAreaSqMm, accuracy: 0.000000001)
        XCTAssertEqual(metrics90.digitalCropRatio, 1.0, accuracy: 0.000000001)
        XCTAssertEqual(metrics90.opticalZoomRatio, 1.2, accuracy: 0.000000001)
        XCTAssertEqual(metrics90.effectiveFNumber, expectedFNumber90, accuracy: 0.000000001)
        XCTAssertGreaterThan(metrics100.opticalActualFocalLengthMm, metrics75.opticalActualFocalLengthMm)
    }

    func testCalculateEffectiveMetrics_cropsOnlyAfterVariableOpticalEnd() {
        let variableTeleLens = LensProcessed(
            nativeFocalLength35mm: 75.0,
            fNumber: 2.39,
            actualFocalLengthMm: 20.0,
            sensorMetrics: wideSensorMetrics,
            opticalEndFocalLength35mm: 100.0,
            endFNumber: 2.96
        )

        let metrics100 = tryUnwrap(calculateEffectiveMetrics(focalLength35mm: 100.0, lenses: [variableTeleLens]))
        let metrics200 = tryUnwrap(calculateEffectiveMetrics(focalLength35mm: 200.0, lenses: [variableTeleLens]))
        let metrics400 = tryUnwrap(calculateEffectiveMetrics(focalLength35mm: 400.0, lenses: [variableTeleLens]))

        XCTAssertEqual(metrics200.effectiveAreaSqMm, metrics100.effectiveAreaSqMm / 4.0, accuracy: 0.000000001)
        XCTAssertEqual(metrics400.effectiveAreaSqMm, metrics100.effectiveAreaSqMm / 16.0, accuracy: 0.000000001)
        XCTAssertEqual(metrics200.digitalCropRatio, 2.0, accuracy: 0.000000001)
        XCTAssertEqual(metrics400.digitalCropRatio, 4.0, accuracy: 0.000000001)
        XCTAssertEqual(metrics200.opticalFocalLength35mm, 100.0, accuracy: 0.0)
    }

    func testComputeProcessedDevice_sortsLensesDropsInvalidOnesAndKeepsReachableFocals() {
        let invalidMetrics = SensorMetrics(
            diagonalMm: 0.0,
            widthMm: 0.0,
            heightMm: 0.0,
            areaSqMm: 0.0,
            sensorName: "Invalid",
            binningType: "None",
            nativePixelSizeUm: 1.0,
            source: .DATABASE
        )
        let processed = tryUnwrap(
            computeProcessedDevice(
                name: "Device A",
                colorHex: "#2563EB",
                rawLenses: [
                    LensProcessingInput(nativeFocalLength35mm: 70.0, fNumber: 2.8, sensorMetrics: teleSensorMetrics),
                    LensProcessingInput(nativeFocalLength35mm: 24.0, fNumber: 2.0, sensorMetrics: invalidMetrics),
                    LensProcessingInput(nativeFocalLength35mm: 24.0, fNumber: 2.0, sensorMetrics: wideSensorMetrics)
                ],
                focalLengths: [14.0, 24.0, 35.0, 70.0]
            )
        )

        XCTAssertEqual(processed.lenses.map { $0.nativeFocalLength35mm }, [24.0, 70.0])
        XCTAssertEqual(processed.metricsByFocalLength.map { $0.focalLength35mm }, [24.0, 35.0, 70.0])
        XCTAssertNil(processed.metricsAt(14.0))
        XCTAssertNotNil(processed.metricsAt(35.0))
    }

    func testComputeProcessedDevice_returnsNilWhenNoUsableLensesRemain() {
        let invalidMetrics = SensorMetrics(
            diagonalMm: 0.0,
            widthMm: 8.0,
            heightMm: 6.0,
            areaSqMm: 0.0,
            sensorName: "Invalid",
            binningType: "None",
            nativePixelSizeUm: 1.0,
            source: .DATABASE
        )

        let processed = computeProcessedDevice(
            name: "Device A",
            colorHex: "#2563EB",
            rawLenses: [LensProcessingInput(nativeFocalLength35mm: 24.0, fNumber: 2.0, sensorMetrics: invalidMetrics)],
            focalLengths: [24.0]
        )

        XCTAssertNil(processed)
    }
}

final class OpticalLensInputTests: XCTestCase {

    func testParseOptionalOpticalLensInput_returnsFixedLensWhenOptionalFieldsAreBlank() {
        let parsed = tryUnwrap(
            parseOptionalOpticalLensInput(
                lens: Self.lens(opticalEndFocalLength: "", endFNumber: ""),
                nativeFocal: 24.0,
                fNumber: 1.8
            )
        )

        XCTAssertEqual(parsed.endFocal, 24.0, accuracy: 0.0)
        XCTAssertEqual(parsed.endFNumber, 1.8, accuracy: 0.0)
    }

    func testParseOptionalOpticalLensInput_defaultsEndFNumberToStartWhenBlank() {
        let parsed = tryUnwrap(
            parseOptionalOpticalLensInput(
                lens: Self.lens(opticalEndFocalLength: "100", endFNumber: ""),
                nativeFocal: 75.0,
                fNumber: 2.39
            )
        )

        XCTAssertEqual(parsed.endFocal, 100.0, accuracy: 0.0)
        XCTAssertEqual(parsed.endFNumber, 2.39, accuracy: 0.0)
    }

    func testParseOptionalOpticalLensInput_parsesValidOpticalZoomEnd() {
        let parsed = tryUnwrap(
            parseOptionalOpticalLensInput(
                lens: Self.lens(opticalEndFocalLength: "100", endFNumber: "2.96"),
                nativeFocal: 75.0,
                fNumber: 2.39
            )
        )

        XCTAssertEqual(parsed.endFocal, 100.0, accuracy: 0.0)
        XCTAssertEqual(parsed.endFNumber, 2.96, accuracy: 0.0)
    }

    func testParseOptionalOpticalLensInput_rejectsEndFNumberWithoutEndFocal() {
        let parsed = parseOptionalOpticalLensInput(
            lens: Self.lens(opticalEndFocalLength: "", endFNumber: "2.96"),
            nativeFocal: 75.0,
            fNumber: 2.39
        )

        XCTAssertNil(parsed)
    }

    func testParseOptionalOpticalLensInput_rejectsInvalidEndFocalOrEndFNumber() {
        let invalidLenses = [
            Self.lens(opticalEndFocalLength: "74.9", endFNumber: "2.96"),
            Self.lens(opticalEndFocalLength: "abc", endFNumber: "2.96"),
            Self.lens(opticalEndFocalLength: "100", endFNumber: "0"),
            Self.lens(opticalEndFocalLength: "100", endFNumber: "-2.96"),
            Self.lens(opticalEndFocalLength: "100", endFNumber: "abc")
        ]

        for invalid in invalidLenses {
            XCTAssertNil(parseOptionalOpticalLensInput(lens: invalid, nativeFocal: 75.0, fNumber: 2.39))
        }
    }

    private static func lens(
        opticalEndFocalLength: String,
        endFNumber: String
    ) -> LensInputState {
        LensInputState(
            id: 1,
            nativeFocalLength: "75",
            selectedSensorValue: MANUAL_INPUT_SENSOR_VALUE,
            manualSensorDescriptor: "1/1.33",
            fNumber: "2.39",
            opticalEndFocalLength: opticalEndFocalLength,
            endFNumber: endFNumber
        )
    }
}

final class GenerateComparisonUseCaseTests: XCTestCase {

    private let useCase = GenerateComparisonUseCase()
    private let defaultFocalLengths = [14.0, 24.0, 35.0, 50.0, 70.0]
    private let databaseSensor = SensorSpec(
        name: "Sony IMX999",
        value: "sensor:sony-imx999",
        megapixels: 50.0,
        pixelSizeUm: 1.0,
        binningType: "None",
        manufacturer: "Sony",
        source: .DATABASE
    )

    func testGenerate_buildsFocalGridFromReachableNativeAndOpticalEndFocals() {
        let output = useCase.generate(
            devices: [
                device(
                    lenses: [
                        lens(
                            nativeFocalLength: "75",
                            fNumber: "2.39",
                            opticalEndFocalLength: "100",
                            endFNumber: "2.96"
                        )
                    ]
                )
            ],
            availableSensors: [databaseSensor],
            selectedFocalLength: 90.0,
            defaultFocalLengths: defaultFocalLengths,
            fallbackDeviceName: { "Device \($0)" }
        )

        XCTAssertNotNil(output.results)
        XCTAssertEqual(output.focalLengths, [75.0, 100.0])
        XCTAssertEqual(output.results?.focalLengths, [75.0, 100.0])
        XCTAssertEqual(output.selectedFocalLength, 100.0, accuracy: 0.0)
    }

    func testGenerate_processesValidLensesWhenOtherLensInputsAreInvalid() {
        let output = useCase.generate(
            devices: [
                device(
                    name: "",
                    lenses: [
                        lens(nativeFocalLength: "bad", fNumber: "1.8"),
                        lens(id: 2, nativeFocalLength: "24", fNumber: "1.8")
                    ]
                )
            ],
            availableSensors: [databaseSensor],
            selectedFocalLength: 35.0,
            defaultFocalLengths: defaultFocalLengths,
            fallbackDeviceName: { "Fallback Device \($0)" }
        )

        let processedDevice = tryUnwrap(output.results?.devices.first)
        XCTAssertEqual(processedDevice.name, "Fallback Device 1")
        XCTAssertEqual(processedDevice.lenses.map { $0.nativeFocalLength35mm }, [24.0])
        XCTAssertEqual(output.focalLengths, [24.0, 35.0, 50.0, 70.0])
        XCTAssertEqual(output.selectedFocalLength, 35.0, accuracy: 0.0)
    }

    func testGenerate_usesManualDescriptorOnlyForManualSensorSelections() {
        let output = useCase.generate(
            devices: [
                device(
                    lenses: [
                        lens(
                            id: 1,
                            nativeFocalLength: "24",
                            selectedSensorValue: databaseSensor.value,
                            manualSensorDescriptor: "not a descriptor",
                            fNumber: "1.8"
                        ),
                        lens(
                            id: 2,
                            nativeFocalLength: "50",
                            selectedSensorValue: MANUAL_INPUT_SENSOR_VALUE,
                            manualSensorDescriptor: "1/1.33",
                            fNumber: "2.0"
                        )
                    ]
                )
            ],
            availableSensors: [databaseSensor],
            selectedFocalLength: 24.0,
            defaultFocalLengths: [24.0, 50.0],
            fallbackDeviceName: { "Device \($0)" }
        )

        let lenses = tryUnwrap(output.results?.devices.first?.lenses)
        XCTAssertEqual(lenses[0].sensorMetrics.sensorName, "Sony IMX999")
        XCTAssertEqual(lenses[1].sensorMetrics.sensorName, "1/1.33")
    }

    func testGenerate_returnsDefaultFocalGridWhenNoDeviceCanBeProcessed() {
        let output = useCase.generate(
            devices: [
                device(lenses: [lens(nativeFocalLength: "bad", fNumber: "1.8")])
            ],
            availableSensors: [databaseSensor],
            selectedFocalLength: 35.0,
            defaultFocalLengths: defaultFocalLengths,
            fallbackDeviceName: { "Device \($0)" }
        )

        XCTAssertNil(output.results)
        XCTAssertEqual(output.focalLengths, defaultFocalLengths)
        XCTAssertEqual(output.selectedFocalLength, defaultFocalLengths.first!, accuracy: 0.0)
    }

    func testGenerate_rejectsManualSensorWithMalformedDescriptor() {
        let output = useCase.generate(
            devices: [
                device(
                    lenses: [
                        lens(
                            selectedSensorValue: MANUAL_INPUT_SENSOR_VALUE,
                            manualSensorDescriptor: "1/1.28/2"
                        )
                    ]
                )
            ],
            availableSensors: [databaseSensor],
            selectedFocalLength: 24.0,
            defaultFocalLengths: defaultFocalLengths,
            fallbackDeviceName: { "Device \($0)" }
        )

        XCTAssertNil(output.results)
    }

    func testGenerate_rejectsEndFNumberWithoutOpticalEndFocalLength() {
        let output = useCase.generate(
            devices: [
                device(lenses: [lens(opticalEndFocalLength: "", endFNumber: "2.96")])
            ],
            availableSensors: [databaseSensor],
            selectedFocalLength: 24.0,
            defaultFocalLengths: defaultFocalLengths,
            fallbackDeviceName: { "Device \($0)" }
        )

        XCTAssertNil(output.results)
    }

    private func device(
        id: Int64 = 1,
        name: String? = nil,
        colorHex: String = "#2563EB",
        lenses: [LensInputState]? = nil
    ) -> DeviceInputState {
        DeviceInputState(
            id: id,
            name: name ?? "Device \(id)",
            colorHex: colorHex,
            lenses: lenses ?? [lens()]
        )
    }

    private func lens(
        id: Int64 = 1,
        nativeFocalLength: String = "24",
        selectedSensorValue: String? = nil,
        manualSensorDescriptor: String = "",
        fNumber: String = "1.8",
        opticalEndFocalLength: String = "",
        endFNumber: String = ""
    ) -> LensInputState {
        LensInputState(
            id: id,
            nativeFocalLength: nativeFocalLength,
            selectedSensorValue: selectedSensorValue ?? databaseSensor.value,
            manualSensorDescriptor: manualSensorDescriptor,
            fNumber: fNumber,
            opticalEndFocalLength: opticalEndFocalLength,
            endFNumber: endFNumber
        )
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
