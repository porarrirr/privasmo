import Foundation

public let MANUAL_INPUT_SENSOR_VALUE = "_MANUAL_INPUT_"
public let INCH_TO_MM = 25.4
public let SENSOR_DIAG_FACTOR = 16.0 / INCH_TO_MM
public let FF_DIAGONAL_MM = 43.2666
public let MAX_DEVICES = 5
public let MAX_LENSES_PER_DEVICE = 4

public let DEFAULT_DEVICE_COLORS = [
    "#2563EB",
    "#7C3AED",
    "#DC2626",
    "#F97316",
    "#059669",
    "#0EA5E9",
    "#F59E0B",
    "#EC4899",
    "#10B981",
    "#6366F1",
    "#14B8A6",
    "#4ADE80"
]

private let manufacturerOrder = ["Sony", "OmniVision", "Samsung", "GalaxyCore", "SmartSens", "Toshiba", "Other"]
private let invariantLocale = Locale(identifier: "en_US_POSIX")
private let trailingAlphaRegex = try! NSRegularExpression(pattern: "[A-Z]+$", options: .caseInsensitive)
private let firstDigitsRegex = try! NSRegularExpression(pattern: "\\d+")
private let manualDescriptorFractionRegex = try! NSRegularExpression(pattern: "^([0-9]+(?:\\.[0-9]+)?)/([0-9]+(?:\\.[0-9]+)?)$")

private struct NumericPattern {
    let regex: NSRegularExpression
    let groupIndex: Int
}

private let numericPatterns: [NumericPattern] = [
    NumericPattern(
        regex: try! NSRegularExpression(
            pattern: "(?:LYT-T?|IMX|S5K[A-Z]{0,2}|OV(?:[A-Z0-9]{2,3})?[A-Z]?|GC|SC|HES|CK|ISOCELL\\s[A-Z]{0,2})(\\d+[A-Z0-9]*)",
            options: .caseInsensitive
        ),
        groupIndex: 1
    ),
    NumericPattern(regex: try! NSRegularExpression(pattern: "(\\d+)MP", options: .caseInsensitive), groupIndex: 1),
    NumericPattern(regex: try! NSRegularExpression(pattern: "[A-Z]+(\\d+)", options: .caseInsensitive), groupIndex: 1),
    NumericPattern(regex: try! NSRegularExpression(pattern: "(\\d+)"), groupIndex: 1)
]

public enum SensorSource: String, Codable {
    case DATABASE
    case MANUAL
}

public struct SensorSpec: Codable, Identifiable, Equatable {
    public var id: String { value }
    public let name: String
    public let value: String
    public let megapixels: Double
    public let pixelSizeUm: Double
    public let binningType: String
    public let manufacturer: String
    public let source: SensorSource
    public var isManual: Bool = false
    
    public init(name: String, value: String, megapixels: Double, pixelSizeUm: Double, binningType: String, manufacturer: String, source: SensorSource, isManual: Bool = false) {
        self.name = name
        self.value = value
        self.megapixels = megapixels
        self.pixelSizeUm = pixelSizeUm
        self.binningType = binningType
        self.manufacturer = manufacturer
        self.source = source
        self.isManual = isManual
    }
}

public struct SensorMetrics: Equatable {
    public let diagonalMm: Double
    public let widthMm: Double
    public let heightMm: Double
    public let areaSqMm: Double
    public let sensorName: String
    public let binningType: String
    public let nativePixelSizeUm: Double
    public let source: SensorSource
}

public struct LensProcessed: Equatable {
    public let nativeFocalLength35mm: Double
    public let fNumber: Double
    public let actualFocalLengthMm: Double
    public let sensorMetrics: SensorMetrics
    public let opticalEndFocalLength35mm: Double
    public let endFNumber: Double

    public init(
        nativeFocalLength35mm: Double,
        fNumber: Double,
        actualFocalLengthMm: Double,
        sensorMetrics: SensorMetrics,
        opticalEndFocalLength35mm: Double? = nil,
        endFNumber: Double? = nil
    ) {
        self.nativeFocalLength35mm = nativeFocalLength35mm
        self.fNumber = fNumber
        self.actualFocalLengthMm = actualFocalLengthMm
        self.sensorMetrics = sensorMetrics
        self.opticalEndFocalLength35mm = opticalEndFocalLength35mm ?? nativeFocalLength35mm
        self.endFNumber = endFNumber ?? fNumber
    }

    public var isVariableOptical: Bool {
        opticalEndFocalLength35mm > nativeFocalLength35mm
    }

    public func containsOptical(_ focal35mm: Double) -> Bool {
        focal35mm >= nativeFocalLength35mm &&
        focal35mm <= opticalEndFocalLength35mm
    }

    public func fNumberAt(_ focal35mm: Double) -> Double {
        let span = opticalEndFocalLength35mm - nativeFocalLength35mm
        if span <= 0.0 { return fNumber }
        let t = min(max((focal35mm - nativeFocalLength35mm) / span, 0.0), 1.0)
        return fNumber + (endFNumber - fNumber) * t
    }
}

public struct FocalLengthMetrics: Equatable {
    public let focalLength35mm: Double
    public let effectiveWidthMm: Double
    public let effectiveHeightMm: Double
    public let effectiveAreaSqMm: Double
    public let zoomRatio: Double
    public let apertureDiameterMm: Double
    public let apertureAreaSqMm: Double
    public let totalLightIntake: Double
    public let baseLens: LensProcessed
    public let opticalFocalLength35mm: Double
    public let opticalZoomRatio: Double
    public let digitalCropRatio: Double
    public let effectiveFNumber: Double
    public let opticalActualFocalLengthMm: Double

    public init(
        focalLength35mm: Double,
        effectiveWidthMm: Double,
        effectiveHeightMm: Double,
        effectiveAreaSqMm: Double,
        zoomRatio: Double,
        apertureDiameterMm: Double,
        apertureAreaSqMm: Double,
        totalLightIntake: Double,
        baseLens: LensProcessed,
        opticalFocalLength35mm: Double? = nil,
        opticalZoomRatio: Double = 1.0,
        digitalCropRatio: Double? = nil,
        effectiveFNumber: Double? = nil,
        opticalActualFocalLengthMm: Double? = nil
    ) {
        self.focalLength35mm = focalLength35mm
        self.effectiveWidthMm = effectiveWidthMm
        self.effectiveHeightMm = effectiveHeightMm
        self.effectiveAreaSqMm = effectiveAreaSqMm
        self.zoomRatio = zoomRatio
        self.apertureDiameterMm = apertureDiameterMm
        self.apertureAreaSqMm = apertureAreaSqMm
        self.totalLightIntake = totalLightIntake
        self.baseLens = baseLens
        self.opticalFocalLength35mm = opticalFocalLength35mm ?? baseLens.nativeFocalLength35mm
        self.opticalZoomRatio = opticalZoomRatio
        self.digitalCropRatio = digitalCropRatio ?? zoomRatio
        self.effectiveFNumber = effectiveFNumber ?? baseLens.fNumber
        self.opticalActualFocalLengthMm = opticalActualFocalLengthMm ?? baseLens.actualFocalLengthMm
    }
}

public struct LensProcessingInput: Equatable {
    public let nativeFocalLength35mm: Double
    public let fNumber: Double
    public let sensorMetrics: SensorMetrics
    public let opticalEndFocalLength35mm: Double
    public let endFNumber: Double

    public init(
        nativeFocalLength35mm: Double,
        fNumber: Double,
        sensorMetrics: SensorMetrics,
        opticalEndFocalLength35mm: Double? = nil,
        endFNumber: Double? = nil
    ) {
        self.nativeFocalLength35mm = nativeFocalLength35mm
        self.fNumber = fNumber
        self.sensorMetrics = sensorMetrics
        self.opticalEndFocalLength35mm = opticalEndFocalLength35mm ?? nativeFocalLength35mm
        self.endFNumber = endFNumber ?? fNumber
    }
}

public struct ProcessedDevice: Equatable, Identifiable {
    public var id: String { name + colorHex }
    public let name: String
    public let colorHex: String
    public let lenses: [LensProcessed]
    public let metricsByFocalLength: [FocalLengthMetrics]
    
    private let metricsLookup: [Double: FocalLengthMetrics]
    
    public init(name: String, colorHex: String, lenses: [LensProcessed], metricsByFocalLength: [FocalLengthMetrics]) {
        self.name = name
        self.colorHex = colorHex
        self.lenses = lenses
        self.metricsByFocalLength = metricsByFocalLength
        
        var lookup: [Double: FocalLengthMetrics] = [:]
        for metric in metricsByFocalLength {
            lookup[metric.focalLength35mm] = metric
        }
        self.metricsLookup = lookup
    }
    
    public func metricsAt(_ focalLength: Double) -> FocalLengthMetrics? {
        return metricsLookup[focalLength]
    }
}

public struct ComparisonResults: Equatable {
    public let focalLengths: [Double]
    public let devices: [ProcessedDevice]
    
    public init(focalLengths: [Double], devices: [ProcessedDevice]) {
        self.focalLengths = focalLengths
        self.devices = devices
    }
}

public func parseSensorCsv(raw: String) -> [SensorSpec] {
    var sensors: [SensorSpec] = []
    
    let lines = raw.components(separatedBy: .newlines)
    for line in lines {
        var trimmed = line.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.hasPrefix("\u{FEFF}") {
            trimmed.removeFirst()
        }
        if trimmed.isEmpty || trimmed.hasPrefix("#") {
            continue
        }
        let parts = parseCsvLine(line: trimmed).map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
        if parts.count != 4 {
            continue
        }
        let name = parts[0]
        guard let mpVal = Double(parts[1]).map({ $0 / 100.0 }),
              let pixelSizeUm = Double(parts[2]) else {
            continue
        }
        let binningType = normalizeBinning(raw: parts[3])
        let manufacturer = detectManufacturer(name: name)
        
        sensors.append(SensorSpec(
            name: name,
            value: name,
            megapixels: mpVal,
            pixelSizeUm: pixelSizeUm,
            binningType: binningType,
            manufacturer: manufacturer,
            source: .DATABASE
        ))
    }
    
    sensors.sort(by: sensorComparator())
    
    sensors.insert(SensorSpec(
        name: LocalizedStrings.labelManualInput,
        value: MANUAL_INPUT_SENSOR_VALUE,
        megapixels: 0.0,
        pixelSizeUm: 0.0,
        binningType: "Manual",
        manufacturer: "Manual",
        source: .MANUAL,
        isManual: true
    ), at: 0)
    
    return sensors
}

private func parseCsvLine(line: String) -> [String] {
    var fields: [String] = []
    var current = ""
    var inQuotes = false
    
    let chars = Array(line)
    var index = 0
    while index < chars.count {
        let char = chars[index]
        if char == "\"" {
            let nextIndex = index + 1
            if inQuotes && nextIndex < chars.count && chars[nextIndex] == "\"" {
                current.append("\"")
                index = nextIndex
            } else {
                inQuotes = !inQuotes
            }
        } else if char == "," {
            if inQuotes {
                current.append(char)
            } else {
                fields.append(current)
                current = ""
            }
        } else {
            current.append(char)
        }
        index += 1
    }
    fields.append(current)
    return fields
}

public func calculateNativeSensorMetrics(sensorSpec: SensorSpec?, manualDescriptor: String?) -> SensorMetrics {
    if let spec = sensorSpec, !spec.isManual {
        if spec.megapixels <= 0.0 || spec.pixelSizeUm <= 0.0 {
            return SensorMetrics(diagonalMm: 0.0, widthMm: 0.0, heightMm: 0.0, areaSqMm: 0.0, sensorName: spec.name, binningType: spec.binningType, nativePixelSizeUm: 0.0, source: .DATABASE)
        }
        let totalPixels = spec.megapixels * 1_000_000.0
        let heightPx = sqrt(totalPixels * 3.0 / 4.0)
        let widthPx = heightPx * 4.0 / 3.0
        let widthMm = widthPx * spec.pixelSizeUm / 1000.0
        let heightMm = heightPx * spec.pixelSizeUm / 1000.0
        let diagonalMm = sqrt(widthMm * widthMm + heightMm * heightMm)
        let areaSqMm = widthMm * heightMm
        return SensorMetrics(
            diagonalMm: diagonalMm,
            widthMm: widthMm,
            heightMm: heightMm,
            areaSqMm: areaSqMm,
            sensorName: spec.name,
            binningType: spec.binningType,
            nativePixelSizeUm: spec.pixelSizeUm,
            source: .DATABASE
        )
    }
    
    let descriptor = (manualDescriptor ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
    guard let diagonalInchNominal = manualDescriptorToDiagonalInches(descriptor: descriptor) else {
        return SensorMetrics(diagonalMm: 0.0, widthMm: 0.0, heightMm: 0.0, areaSqMm: 0.0, sensorName: descriptor.isEmpty ? "N/A" : descriptor, binningType: "Manual", nativePixelSizeUm: 0.0, source: .MANUAL)
    }
    let diagonalMmOptical = diagonalInchNominal * INCH_TO_MM * SENSOR_DIAG_FACTOR
    let widthMm = (4.0 / 5.0) * diagonalMmOptical
    let heightMm = (3.0 / 5.0) * diagonalMmOptical
    let areaSqMm = widthMm * heightMm
    
    return SensorMetrics(
        diagonalMm: diagonalMmOptical,
        widthMm: widthMm,
        heightMm: heightMm,
        areaSqMm: areaSqMm,
        sensorName: descriptor,
        binningType: "Manual",
        nativePixelSizeUm: 0.0,
        source: .MANUAL
    )
}

public func computeProcessedDevice(
    name: String,
    colorHex: String,
    rawLenses: [LensProcessingInput],
    focalLengths: [Double]
) -> ProcessedDevice? {
    if rawLenses.isEmpty { return nil }
    
    var lenses: [LensProcessed] = []
    for lens in rawLenses {
        let focalLength35 = lens.nativeFocalLength35mm
        let metrics = lens.sensorMetrics
        if metrics.diagonalMm > 0.0 {
            let cropFactor = FF_DIAGONAL_MM / metrics.diagonalMm
            let actualFocal = focalLength35 / cropFactor
            lenses.append(LensProcessed(
                nativeFocalLength35mm: focalLength35,
                fNumber: lens.fNumber,
                actualFocalLengthMm: actualFocal,
                sensorMetrics: metrics,
                opticalEndFocalLength35mm: lens.opticalEndFocalLength35mm,
                endFNumber: lens.endFNumber
            ))
        }
    }
    
    lenses.sort {
        if $0.nativeFocalLength35mm == $1.nativeFocalLength35mm {
            return $0.opticalEndFocalLength35mm < $1.opticalEndFocalLength35mm
        }
        return $0.nativeFocalLength35mm < $1.nativeFocalLength35mm
    }
    if lenses.isEmpty { return nil }
    
    var metricsByFocalLength: [FocalLengthMetrics] = []
    for currentFocal in focalLengths {
        if let metrics = calculateEffectiveMetrics(focalLength35mm: currentFocal, lenses: lenses) {
            metricsByFocalLength.append(metrics)
        }
    }
    
    return ProcessedDevice(
        name: name,
        colorHex: colorHex,
        lenses: lenses,
        metricsByFocalLength: metricsByFocalLength
    )
}

public func calculateEffectiveMetrics(focalLength35mm: Double, lenses: [LensProcessed]) -> FocalLengthMetrics? {
    guard !lenses.isEmpty else { return nil }
    let sorted = lenses.sorted {
        if $0.nativeFocalLength35mm == $1.nativeFocalLength35mm {
            return $0.opticalEndFocalLength35mm < $1.opticalEndFocalLength35mm
        }
        return $0.nativeFocalLength35mm < $1.nativeFocalLength35mm
    }
    let opticalLens = sorted
        .filter { $0.containsOptical(focalLength35mm) }
        .max {
            if $0.nativeFocalLength35mm == $1.nativeFocalLength35mm {
                return $0.opticalEndFocalLength35mm < $1.opticalEndFocalLength35mm
            }
            return $0.nativeFocalLength35mm < $1.nativeFocalLength35mm
        }
    let baseLens: LensProcessed
    if let activeOpticalLens = opticalLens {
        baseLens = activeOpticalLens
    } else if let fallback = sorted
        .filter({ focalLength35mm >= $0.opticalEndFocalLength35mm })
        .max(by: {
            if $0.opticalEndFocalLength35mm == $1.opticalEndFocalLength35mm {
                return $0.nativeFocalLength35mm < $1.nativeFocalLength35mm
            }
            return $0.opticalEndFocalLength35mm < $1.opticalEndFocalLength35mm
        }) {
        baseLens = fallback
    } else {
        return nil
    }
    
    let opticalFocal35mm: Double
    if opticalLens == nil {
        opticalFocal35mm = baseLens.opticalEndFocalLength35mm
    } else {
        opticalFocal35mm = focalLength35mm
    }
    let digitalCropRatio = max(1.0, focalLength35mm / opticalFocal35mm)
    let effectiveWidthMm = baseLens.sensorMetrics.widthMm / digitalCropRatio
    let effectiveHeightMm = baseLens.sensorMetrics.heightMm / digitalCropRatio
    let effectiveAreaSqMm = baseLens.sensorMetrics.areaSqMm / pow(digitalCropRatio, 2)
    
    let cropFactor = FF_DIAGONAL_MM / baseLens.sensorMetrics.diagonalMm
    let opticalActualFocalMm = opticalFocal35mm / cropFactor
    let effectiveFNumber = baseLens.fNumberAt(opticalFocal35mm)
    let apertureDiameter = effectiveFNumber > 0 ? opticalActualFocalMm / effectiveFNumber : 0.0
    let apertureArea = apertureDiameter > 0 ? (Double.pi / 4.0) * pow(apertureDiameter, 2) : 0.0
    let totalLightIntake = effectiveFNumber > 0 ? effectiveAreaSqMm / pow(effectiveFNumber, 2) : 0.0
    let opticalZoomRatio = opticalFocal35mm / baseLens.nativeFocalLength35mm
    
    return FocalLengthMetrics(
        focalLength35mm: focalLength35mm,
        effectiveWidthMm: effectiveWidthMm,
        effectiveHeightMm: effectiveHeightMm,
        effectiveAreaSqMm: effectiveAreaSqMm,
        zoomRatio: digitalCropRatio,
        apertureDiameterMm: apertureDiameter,
        apertureAreaSqMm: apertureArea,
        totalLightIntake: totalLightIntake,
        baseLens: baseLens,
        opticalFocalLength35mm: opticalFocal35mm,
        opticalZoomRatio: opticalZoomRatio,
        digitalCropRatio: digitalCropRatio,
        effectiveFNumber: effectiveFNumber,
        opticalActualFocalLengthMm: opticalActualFocalMm
    )
}

public func isValidManualSensorDescriptor(descriptor: String) -> Bool {
    return manualDescriptorToDiagonalInches(descriptor: descriptor.trimmingCharacters(in: .whitespacesAndNewlines)) != nil
}

private func manualDescriptorToDiagonalInches(descriptor: String) -> Double? {
    if descriptor.isEmpty { return nil }
    
    var normalized = ""
    for char in descriptor {
        let ascii: Character
        switch char {
        case "０"..."９":
            let code = Int(char.unicodeScalars.first!.value) - Int(Character("０").unicodeScalars.first!.value) + Int(Character("0").unicodeScalars.first!.value)
            ascii = Character(UnicodeScalar(code)!)
        case "／":
            ascii = "/"
        case "．", "。", "，", ",":
            ascii = "."
        default:
            ascii = char
        }
        normalized.append(ascii)
    }
    let compact = normalized.replacingOccurrences(of: "\\s+", with: "", options: .regularExpression)
    
    let range = NSRange(location: 0, length: compact.utf16.count)
    guard let match = manualDescriptorFractionRegex.firstMatch(in: compact, options: [], range: range) else {
        return nil
    }
    
    guard let numRange = Range(match.range(at: 1), in: compact),
          let denRange = Range(match.range(at: 2), in: compact),
          let numerator = Double(compact[numRange]), numerator > 0.0,
          let denominator = Double(compact[denRange]), denominator > 0.0 else {
        return nil
    }
    
    return numerator / denominator
}

private func sensorComparator() -> (SensorSpec, SensorSpec) -> Bool {
    return { a, b in
        let idxA = manufacturerOrder.firstIndex(of: a.manufacturer) ?? manufacturerOrder.count
        let idxB = manufacturerOrder.firstIndex(of: b.manufacturer) ?? manufacturerOrder.count
        if idxA != idxB {
            return idxA < idxB
        }
        
        if a.manufacturer == "Sony" && b.manufacturer == "Sony" {
            let isALyt = a.name.hasPrefix("Sony LYT")
            let isBLyt = b.name.hasPrefix("Sony LYT")
            let isAImx = a.name.hasPrefix("Sony IMX")
            let isBImx = b.name.hasPrefix("Sony IMX")
            
            if isALyt && !isBLyt { return true }
            if !isALyt && isBLyt { return false }
            if isALyt && isBLyt {
                let numA = getNumericPartForSort(String(a.name.dropFirst("Sony LYT".count)))
                let numB = getNumericPartForSort(String(b.name.dropFirst("Sony LYT".count)))
                if numA != numB { return numA > numB }
            }
            if isAImx && !isBImx && !isBLyt { return true }
            if !isAImx && !isALyt && isBImx { return false }
        }
        
        let numA = getNumericPartForSort(a.name)
        let numB = getNumericPartForSort(b.name)
        if numA != numB && numA != 0 && numB != 0 {
            return numA > numB
        }
        
        return a.name.compare(b.name, options: .numeric) == .orderedAscending
    }
}

private func getNumericPartForSort(_ name: String) -> Int {
    for pattern in numericPatterns {
        let range = NSRange(location: 0, length: name.utf16.count)
        guard let match = pattern.regex.firstMatch(in: name, options: [], range: range) else {
            continue
        }
        guard let matchRange = Range(match.range(at: pattern.groupIndex), in: name) else {
            continue
        }
        let groupValue = String(name[matchRange])
        
        let withoutSuffixRange = NSRange(location: 0, length: groupValue.utf16.count)
        let withoutSuffix = trailingAlphaRegex.stringByReplacingMatches(in: groupValue, options: [], range: withoutSuffixRange, withTemplate: "")
        
        if !withoutSuffix.isEmpty && withoutSuffix.allSatisfy({ $0.isNumber }) {
            if let val = Int(withoutSuffix) {
                return val
            }
        }
        
        let digitsRange = NSRange(location: 0, length: groupValue.utf16.count)
        if let digitsMatch = firstDigitsRegex.firstMatch(in: groupValue, options: [], range: digitsRange),
           let digitsRangeInValue = Range(digitsMatch.range, in: groupValue),
           let val = Int(groupValue[digitsRangeInValue]) {
            return val
        }
    }
    return 0
}

internal func normalizeBinning(raw: String) -> String {
    let binningRaw = raw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(with: invariantLocale)
    if binningRaw == "yes" {
        return "Quad Bayer (2x2)"
    } else if binningRaw.contains("nona") {
        return "Nona (3x3)"
    } else if binningRaw.contains("16-cell") || binningRaw.contains("16-in-1") {
        return "16-cell (4x4)"
    } else if binningRaw == "no" {
        return "None"
    } else if binningRaw == "unknown" {
        return "Unknown"
    } else {
        return raw.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

internal func detectManufacturer(name: String) -> String {
    let lower = name.lowercased(with: invariantLocale)
    if lower.contains("sony") || name.contains("ソニー") {
        return "Sony"
    } else if lower.contains("omnivision") {
        return "OmniVision"
    } else if lower.contains("samsung") {
        return "Samsung"
    } else if lower.contains("galaxycore") {
        return "GalaxyCore"
    } else if lower.contains("smartsens") {
        return "SmartSens"
    } else if lower.contains("toshiba") || name.contains("東芝") {
        return "Toshiba"
    } else {
        return "Other"
    }
}
