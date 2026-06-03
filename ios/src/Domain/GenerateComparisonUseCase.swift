import Foundation

public struct GeneratedComparison: Equatable {
    public let results: ComparisonResults?
    public let focalLengths: [Double]
    public let selectedFocalLength: Double
}

struct ParsedOpticalLensInput {
    let endFocal: Double
    let endFNumber: Double
}

func parseOptionalOpticalLensInput(
    lens: LensInputState,
    nativeFocal: Double,
    fNumber: Double
) -> ParsedOpticalLensInput? {
    let endFocalText = lens.opticalEndFocalLength.trimmingCharacters(in: .whitespacesAndNewlines)
    let endFNumberText = lens.endFNumber.trimmingCharacters(in: .whitespacesAndNewlines)
    if endFocalText.isEmpty {
        return endFNumberText.isEmpty
            ? ParsedOpticalLensInput(endFocal: nativeFocal, endFNumber: fNumber)
            : nil
    }

    guard let endFocal = Double(endFocalText), endFocal >= nativeFocal else {
        return nil
    }
    let endFNumber: Double
    if endFNumberText.isEmpty {
        endFNumber = fNumber
    } else {
        guard let parsedEndFNumber = Double(endFNumberText), parsedEndFNumber > 0.0 else {
            return nil
        }
        endFNumber = parsedEndFNumber
    }
    return ParsedOpticalLensInput(endFocal: endFocal, endFNumber: endFNumber)
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
            device.lenses.flatMap { lens -> [Double] in
                guard let focal = Double(lens.nativeFocalLength), focal > 0.0 else {
                    return []
                }
                guard let fNumber = Double(lens.fNumber), fNumber > 0.0 else {
                    return [focal]
                }
                let opticalEnd = parseOptionalOpticalLensInput(
                    lens: lens,
                    nativeFocal: focal,
                    fNumber: fNumber
                )?.endFocal
                return [
                    focal,
                    opticalEnd.flatMap { $0 == focal ? nil : $0 }
                ].compactMap { $0 }
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
            
            var rawLenses: [LensProcessingInput] = []
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
                guard let optical = parseOptionalOpticalLensInput(lens: lens, nativeFocal: focal, fNumber: fNumber) else {
                    continue
                }
                rawLenses.append(LensProcessingInput(
                    nativeFocalLength35mm: focal,
                    fNumber: fNumber,
                    sensorMetrics: metrics,
                    opticalEndFocalLength35mm: optical.endFocal,
                    endFNumber: optical.endFNumber
                ))
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
