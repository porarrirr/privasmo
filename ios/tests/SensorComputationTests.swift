import XCTest
@testable import sumahohikakuku

final class SensorComputationTests: XCTestCase {
    
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
    
    private var lenses: [LensProcessed] {
        [wideLens, teleLens]
    }

    private var xiaomi17UltraTeleLens: LensProcessed {
        LensProcessed(
            nativeFocalLength35mm: 75.0,
            fNumber: 2.39,
            actualFocalLengthMm: 20.0,
            sensorMetrics: wideSensorMetrics,
            opticalEndFocalLength35mm: 100.0,
            endFNumber: 2.96
        )
    }

    private var xiaomi17UltraTeleLenses: [LensProcessed] {
        [xiaomi17UltraTeleLens]
    }

    private var variableWideLens: LensProcessed {
        LensProcessed(
            nativeFocalLength35mm: wideLens.nativeFocalLength35mm,
            fNumber: wideLens.fNumber,
            actualFocalLengthMm: wideLens.actualFocalLengthMm,
            sensorMetrics: wideLens.sensorMetrics,
            opticalEndFocalLength35mm: 70.0,
            endFNumber: 2.8
        )
    }
    
    func testCalculateEffectiveMetrics_returnsUnityZoomAtNativeFocal() {
        let metrics = calculateEffectiveMetrics(focalLength35mm: 24.0, lenses: lenses)
        
        XCTAssertNotNil(metrics)
        XCTAssertEqual(metrics?.zoomRatio ?? 0.0, 1.0, accuracy: 1e-9)
        XCTAssertEqual(metrics?.effectiveAreaSqMm ?? 0.0, 48.0, accuracy: 1e-9)
        XCTAssertEqual(metrics?.totalLightIntake ?? 0.0, 12.0, accuracy: 1e-9)
    }
    
    func testCalculateEffectiveMetrics_appliesInverseSquareAreaForDigitalZoom() {
        let metrics = calculateEffectiveMetrics(focalLength35mm: 48.0, lenses: lenses)
        
        XCTAssertNotNil(metrics)
        XCTAssertEqual(metrics?.zoomRatio ?? 0.0, 2.0, accuracy: 1e-9)
        XCTAssertEqual(metrics?.effectiveAreaSqMm ?? 0.0, 12.0, accuracy: 1e-9)
        XCTAssertEqual(metrics?.totalLightIntake ?? 0.0, 3.0, accuracy: 1e-9)
    }
    
    func testCalculateEffectiveMetrics_switchesBaseLensAtBoundary() {
        let beforeBoundary = calculateEffectiveMetrics(focalLength35mm: 69.0, lenses: lenses)
        let atBoundary = calculateEffectiveMetrics(focalLength35mm: 70.0, lenses: lenses)
        
        XCTAssertNotNil(beforeBoundary)
        XCTAssertNotNil(atBoundary)
        XCTAssertEqual(beforeBoundary?.baseLens.actualFocalLengthMm ?? 0.0, 5.5, accuracy: 1e-9)
        XCTAssertEqual(atBoundary?.baseLens.actualFocalLengthMm ?? 0.0, 12.9, accuracy: 1e-9)
    }
    
    func testComputeProcessedDevice_returnsNilWhenAllLensesAreInvalid() {
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
        let rawLenses = [
            LensProcessingInput(
                nativeFocalLength35mm: 24.0,
                fNumber: 2.0,
                sensorMetrics: invalidMetrics
            )
        ]
        
        let processed = computeProcessedDevice(
            name: "Device A",
            colorHex: "#2563EB",
            rawLenses: rawLenses,
            focalLengths: [24.0, 35.0]
        )
        
        XCTAssertNil(processed)
    }
    
    func testComputeProcessedDevice_generatesMetricsForReachableFocals() {
        let rawLenses = [
            LensProcessingInput(
                nativeFocalLength35mm: 24.0,
                fNumber: 2.0,
                sensorMetrics: wideSensorMetrics
            ),
            LensProcessingInput(
                nativeFocalLength35mm: 70.0,
                fNumber: 2.8,
                sensorMetrics: teleSensorMetrics
            )
        ]
        
        let processed = computeProcessedDevice(
            name: "Device A",
            colorHex: "#2563EB",
            rawLenses: rawLenses,
            focalLengths: [14.0, 24.0, 35.0, 70.0]
        )
        
        XCTAssertNotNil(processed)
        XCTAssertEqual(
            processed?.metricsByFocalLength.map { $0.focalLength35mm },
            [24.0, 35.0, 70.0]
        )
    }

    func testXiaomi17UltraTelephoto_keepsFullAreaWithin75To100mm() {
        let metrics75 = calculateEffectiveMetrics(focalLength35mm: 75.0, lenses: xiaomi17UltraTeleLenses)!
        let metrics90 = calculateEffectiveMetrics(focalLength35mm: 90.0, lenses: xiaomi17UltraTeleLenses)!
        let metrics100 = calculateEffectiveMetrics(focalLength35mm: 100.0, lenses: xiaomi17UltraTeleLenses)!

        XCTAssertEqual(metrics75.effectiveAreaSqMm, metrics90.effectiveAreaSqMm, accuracy: 0.0001)
        XCTAssertEqual(metrics75.effectiveAreaSqMm, metrics100.effectiveAreaSqMm, accuracy: 0.0001)
        XCTAssertEqual(metrics90.digitalCropRatio, 1.0, accuracy: 0.0001)
        XCTAssertEqual(metrics90.opticalFocalLength35mm, 90.0, accuracy: 0.0001)
    }

    func testXiaomi17UltraTelephoto_cropsAfter100mm() {
        let metrics100 = calculateEffectiveMetrics(focalLength35mm: 100.0, lenses: xiaomi17UltraTeleLenses)!
        let metrics200 = calculateEffectiveMetrics(focalLength35mm: 200.0, lenses: xiaomi17UltraTeleLenses)!
        let metrics400 = calculateEffectiveMetrics(focalLength35mm: 400.0, lenses: xiaomi17UltraTeleLenses)!

        XCTAssertEqual(metrics200.effectiveAreaSqMm, metrics100.effectiveAreaSqMm / 4.0, accuracy: 0.0001)
        XCTAssertEqual(metrics400.effectiveAreaSqMm, metrics100.effectiveAreaSqMm / 16.0, accuracy: 0.0001)
        XCTAssertEqual(metrics200.digitalCropRatio, 2.0, accuracy: 0.0001)
        XCTAssertEqual(metrics400.digitalCropRatio, 4.0, accuracy: 0.0001)
        XCTAssertEqual(metrics200.opticalFocalLength35mm, 100.0, accuracy: 0.0001)
    }

    func testXiaomi17UltraTelephoto_interpolatesFNumberWithinRange() {
        let metrics75 = calculateEffectiveMetrics(focalLength35mm: 75.0, lenses: xiaomi17UltraTeleLenses)!
        let metrics100 = calculateEffectiveMetrics(focalLength35mm: 100.0, lenses: xiaomi17UltraTeleLenses)!
        let metrics90 = calculateEffectiveMetrics(focalLength35mm: 90.0, lenses: xiaomi17UltraTeleLenses)!
        let expected90 = 2.39 + (2.96 - 2.39) * ((90.0 - 75.0) / (100.0 - 75.0))

        XCTAssertEqual(metrics75.effectiveFNumber, 2.39, accuracy: 0.0001)
        XCTAssertEqual(metrics100.effectiveFNumber, 2.96, accuracy: 0.0001)
        XCTAssertEqual(metrics90.effectiveFNumber, expected90, accuracy: 0.0001)
    }

    func testXiaomi17UltraTelephoto_actualFocalChangesWithinOpticalRange() {
        let metrics75 = calculateEffectiveMetrics(focalLength35mm: 75.0, lenses: xiaomi17UltraTeleLenses)!
        let metrics100 = calculateEffectiveMetrics(focalLength35mm: 100.0, lenses: xiaomi17UltraTeleLenses)!

        XCTAssertGreaterThan(metrics100.opticalActualFocalLengthMm, metrics75.opticalActualFocalLengthMm)
    }

    func testCalculateEffectiveMetrics_prefersNativeLensAtOpticalRangeBoundary() {
        let metrics = calculateEffectiveMetrics(
            focalLength35mm: 70.0,
            lenses: [variableWideLens, teleLens]
        )

        XCTAssertEqual(metrics?.baseLens.nativeFocalLength35mm, 70.0, accuracy: 0.0001)
    }
}
