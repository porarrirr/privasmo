import Foundation

public struct PresetLensSnapshot: Codable, Equatable {
    public let nativeFocalLength: String
    public let selectedSensorValue: String
    public let manualSensorDescriptor: String
    public let fNumber: String
    
    public init(nativeFocalLength: String, selectedSensorValue: String, manualSensorDescriptor: String, fNumber: String) {
        self.nativeFocalLength = nativeFocalLength
        self.selectedSensorValue = selectedSensorValue
        self.manualSensorDescriptor = manualSensorDescriptor
        self.fNumber = fNumber
    }
}

public struct PresetDeviceSnapshot: Codable, Equatable {
    public let name: String
    public let colorHex: String
    public let lenses: [PresetLensSnapshot]
    
    public init(name: String, colorHex: String, lenses: [PresetLensSnapshot]) {
        self.name = name
        self.colorHex = colorHex
        self.lenses = lenses
    }
}

public struct PresetSnapshot: Codable, Equatable, Identifiable {
    public let id: String
    public let name: String
    public let device: PresetDeviceSnapshot
    public let createdAtEpochMillis: Int64
    public let updatedAtEpochMillis: Int64
    
    public init(id: String, name: String, device: PresetDeviceSnapshot, createdAtEpochMillis: Int64 = 0, updatedAtEpochMillis: Int64 = 0) {
        self.id = id
        self.name = name
        self.device = device
        self.createdAtEpochMillis = createdAtEpochMillis
        self.updatedAtEpochMillis = updatedAtEpochMillis
    }
}
