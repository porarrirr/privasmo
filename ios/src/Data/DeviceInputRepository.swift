import Foundation

public struct SavedLensInput: Codable, Equatable {
    public let nativeFocalLength: String
    public let selectedSensorValue: String
    public let manualSensorDescriptor: String
    public let fNumber: String
    
    public init(nativeFocalLength: String = "", selectedSensorValue: String = MANUAL_INPUT_SENSOR_VALUE, manualSensorDescriptor: String = "", fNumber: String = "") {
        self.nativeFocalLength = nativeFocalLength
        self.selectedSensorValue = selectedSensorValue
        self.manualSensorDescriptor = manualSensorDescriptor
        self.fNumber = fNumber
    }
}

public struct SavedDeviceInput: Codable, Equatable {
    public let name: String
    public let colorHex: String
    public let lenses: [SavedLensInput]
    
    public init(name: String = "", colorHex: String = "", lenses: [SavedLensInput] = []) {
        self.name = name
        self.colorHex = colorHex
        self.lenses = lenses
    }
}

public class DeviceInputRepository {
    private let userDefaultsKey = "sensor_comparison_inputs"
    
    public init() {}
    
    public func load() -> Result<[SavedDeviceInput]?, Error> {
        guard let data = UserDefaults.standard.data(forKey: userDefaultsKey) else {
            return .success(nil)
        }
        do {
            let decoder = JSONDecoder()
            let list = try decoder.decode([SavedDeviceInput].self, from: data)
            return .success(list.isEmpty ? nil : list)
        } catch {
            return .failure(error)
        }
    }
    
    public func save(_ devices: [SavedDeviceInput]) -> Result<Void, Error> {
        if devices.isEmpty {
            UserDefaults.standard.removeObject(forKey: userDefaultsKey)
            return .success(())
        }
        do {
            let encoder = JSONEncoder()
            let data = try encoder.encode(devices)
            UserDefaults.standard.set(data, forKey: userDefaultsKey)
            return .success(())
        } catch {
            return .failure(error)
        }
    }
}
