import Foundation
import Combine

public class CustomSensorRepository: ObservableObject {
    @Published public var sensors: [CustomSensorEntry] = []
    private let userDefaultsKey = "custom_sensors"
    
    public init() {
        loadSensors()
    }
    
    private func loadSensors() {
        guard let data = UserDefaults.standard.data(forKey: userDefaultsKey) else {
            self.sensors = []
            return
        }
        do {
            let decoder = JSONDecoder()
            let list = try decoder.decode([CustomSensorEntry].self, from: data)
            self.sensors = list.sorted(by: { $0.name.lowercased() < $1.name.lowercased() })
        } catch {
            print("Failed to decode custom sensors: \(error)")
            self.sensors = []
        }
    }
    
    private func saveSensors() {
        do {
            let encoder = JSONEncoder()
            let data = try encoder.encode(sensors)
            UserDefaults.standard.set(data, forKey: userDefaultsKey)
        } catch {
            print("Failed to encode custom sensors: \(error)")
        }
    }
    
    public func upsertSensor(_ sensor: CustomSensorEntry) {
        if let idx = sensors.firstIndex(where: { $0.id == sensor.id }) {
            sensors[idx] = sensor
        } else {
            sensors.append(sensor)
        }
        sensors.sort(by: { $0.name.lowercased() < $1.name.lowercased() })
        saveSensors()
    }
    
    public func deleteSensor(id: String) {
        sensors.removeAll(where: { $0.id == id })
        saveSensors()
    }
}
