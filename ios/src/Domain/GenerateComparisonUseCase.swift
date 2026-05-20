import Foundation

public struct GeneratedComparison: Equatable {
    public let results: ComparisonResults?
    public let focalLengths: [Double]
    public let selectedFocalLength: Double
}

public class GenerateComparisonUseCase {
    public init() {}
    
    public func generate(
        devices: [DeviceInputState],
        availableSensors: [SensorSpec],
        selectedFocalLength: Double,
        defaultFocalLengths: [Double],
        fallbackDeviceName: (Int) -> String
    ) -> GeneratedComparison {
        var sensorLookup: [String: SensorSpec] = [:]
        for sensor in availableSensors {
            sensorLookup[sensor.value] = sensor
        }
        
        let nativeFocals = devices.flatMap { device in
            device.lenses.compactMap { lens in
                Double(lens.nativeFocalLength).flatMap { $0 > 0.0 ? $0 : nil }
            }
        }
        
        var focalGridSet = Set<Double>()
        for f in defaultFocalLengths {
            focalGridSet.insert(f)
        }
        for f in nativeFocals {
            focalGridSet.insert(f)
        }
        let focalGrid = focalGridSet.sorted()
        
        var processedDevices: [ProcessedDevice] = []
        for (index, device) in devices.enumerated() {
            let name = device.name.trimmingCharacters(in: .whitespacesAndNewlines)
            let sanitizedName = name.isEmpty ? fallbackDeviceName(index + 1) : name
            
            var rawLenses: [(Double, (Double, SensorMetrics))] = []
            for lens in device.lenses {
                guard let focal = Double(lens.nativeFocalLength), focal > 0.0,
                      let fNumber = Double(lens.fNumber), fNumber > 0.0 else {
                    continue
                }
                let sensorSpec = sensorLookup[lens.selectedSensorValue]
                let manualDescriptor = lens.usesManualSensor ? lens.manualSensorDescriptor : nil
                let metrics = calculateNativeSensorMetrics(sensorSpec: sensorSpec, manualDescriptor: manualDescriptor)
                if metrics.areaSqMm <= 0.0 || metrics.diagonalMm <= 0.0 {
                    continue
                }
                rawLenses.append((focal, (fNumber, metrics)))
            }
            
            if let processed = computeProcessedDevice(name: sanitizedName, colorHex: device.colorHex, rawLenses: rawLenses, focalLengths: focalGrid) {
                processedDevices.append(processed)
            }
        }
        
        if processedDevices.isEmpty {
            return GeneratedComparison(
                results: nil,
                focalLengths: defaultFocalLengths,
                selectedFocalLength: defaultFocalLengths.first ?? 14.0
            )
        }
        
        var availableFocalLengthsSet = Set<Double>()
        for device in processedDevices {
            for metric in device.metricsByFocalLength {
                availableFocalLengthsSet.insert(metric.focalLength35mm)
            }
        }
        let availableFocalLengths = availableFocalLengthsSet.sorted()
        
        if availableFocalLengths.isEmpty {
            return GeneratedComparison(
                results: nil,
                focalLengths: defaultFocalLengths,
                selectedFocalLength: defaultFocalLengths.first ?? 14.0
            )
        }
        
        var nearestFocal = availableFocalLengths.first!
        var minDiff = abs(nearestFocal - selectedFocalLength)
        for focal in availableFocalLengths {
            let diff = abs(focal - selectedFocalLength)
            if diff < minDiff {
                minDiff = diff
                nearestFocal = focal
            }
        }
        
        return GeneratedComparison(
            results: ComparisonResults(
                focalLengths: availableFocalLengths,
                devices: processedDevices
            ),
            focalLengths: availableFocalLengths,
            selectedFocalLength: nearestFocal
        )
    }
}
