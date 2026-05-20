import XCTest
@testable import sumahohikakuku

final class SensorModelsTests: XCTestCase {
    
    func testParseSensorCsv_handlesQuotedFields() {
        let raw = "\"Sony, IMX999\",5000,1.0,No"
        let sensors = parseSensorCsv(raw: raw)
        let sensor = sensors.first { !$0.isManual }
        
        XCTAssertNotNil(sensor)
        XCTAssertEqual(sensor?.name, "Sony, IMX999")
        XCTAssertEqual(sensor?.megapixels ?? 0.0, 50.0, accuracy: 0.001)
    }
    
    func testIsValidManualSensorDescriptor_acceptsCommaDecimal() {
        XCTAssertTrue(isValidManualSensorDescriptor(descriptor: "1/1,33"))
    }
    
    func testIsValidManualSensorDescriptor_acceptsFullWidthDigitsAndSlash() {
        XCTAssertTrue(isValidManualSensorDescriptor(descriptor: "１／１．３３"))
    }
    
    func testIsValidManualSensorDescriptor_rejectsMalformedFractions() {
        let invalidInputs = [
            "abc/1.28",
            "1//1.28",
            "1/1.28/2",
            "/1.28",
            "1/",
            "1/0"
        ]
        
        for input in invalidInputs {
            XCTAssertFalse(isValidManualSensorDescriptor(descriptor: input), "Expected invalid manual descriptor: \(input)")
        }
    }
    
    func testCalculateNativeSensorMetrics_handlesCommaDecimal() {
        let metrics = calculateNativeSensorMetrics(sensorSpec: nil, manualDescriptor: "1/1,33")
        XCTAssertGreaterThan(metrics.areaSqMm, 0.0)
        XCTAssertEqual(metrics.sensorName, "1/1,33")
    }
    
    func testCalculateNativeSensorMetrics_returnsZeroAreaForMalformedManualDescriptor() {
        let metrics = calculateNativeSensorMetrics(sensorSpec: nil, manualDescriptor: "1/1.28/2")
        XCTAssertEqual(metrics.areaSqMm, 0.0, accuracy: 0.001)
    }
    
    func testCalculateNativeSensorMetrics_handlesFullWidthDigitsAndPreservesLabel() {
        let metrics = calculateNativeSensorMetrics(sensorSpec: nil, manualDescriptor: "１／１．３３")
        XCTAssertGreaterThan(metrics.areaSqMm, 0.0)
        XCTAssertEqual(metrics.sensorName, "１／１．３３")
    }
    
    func testParseSensorCsv_interpretsMegapixelColumnAsHundredBasedValue() {
        let raw = "Test Sensor,50,1.0,No"
        let sensors = parseSensorCsv(raw: raw)
        let sensor = sensors.first { !$0.isManual }
        
        XCTAssertNotNil(sensor)
        XCTAssertEqual(sensor?.megapixels ?? 0.0, 0.5, accuracy: 0.001)
    }
    
    func testNormalizeBinning_isLocaleIndependent() {
        XCTAssertEqual(normalizeBinning(raw: "16-IN-1"), "16-cell (4x4)")
    }
    
    func testDetectManufacturer_isLocaleIndependent() {
        XCTAssertEqual(detectManufacturer(name: "OMNIVISION OV50"), "OmniVision")
    }
}
