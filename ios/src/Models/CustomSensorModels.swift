import Foundation

public struct CustomSensorEntry: Codable, Equatable, Identifiable {
    public let id: String
    public let name: String
    public let megapixels: Double
    public let pixelSizeUm: Double
    public let binningType: String
    
    public init(id: String, name: String, megapixels: Double, pixelSizeUm: Double, binningType: String) {
        self.id = id
        self.name = name
        self.megapixels = megapixels
        self.pixelSizeUm = pixelSizeUm
        self.binningType = binningType
    }
}

extension CustomSensorEntry {
    public func toSensorSpec() -> SensorSpec {
        return SensorSpec(
            name: name,
            value: id,
            megapixels: megapixels,
            pixelSizeUm: pixelSizeUm,
            binningType: normalizeBinning(raw: binningType),
            manufacturer: detectManufacturer(name: name),
            source: .DATABASE
        )
    }
}

