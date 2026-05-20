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
            (24.0, (2.0, invalidMetrics))
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
            (24.0, (2.0, wideSensorMetrics)),
            (70.0, (2.8, teleSensorMetrics))
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
}
