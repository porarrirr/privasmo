import Foundation
import Combine

public class PresetRepository: ObservableObject {
    @Published public var presets: [PresetSnapshot] = []
    private let userDefaultsKey = "sensor_presets"
    
    public init() {
        loadPresets()
    }
    
    private func loadPresets() {
        guard let data = UserDefaults.standard.data(forKey: userDefaultsKey) else {
            self.presets = []
            return
        }
        do {
            let decoder = JSONDecoder()
            let list = try decoder.decode([PresetSnapshot].self, from: data)
            self.presets = list.sorted(by: { $0.name.lowercased() < $1.name.lowercased() })
        } catch {
            print("Failed to decode presets: \(error)")
            self.presets = []
        }
    }
    
    private func savePresets() {
        do {
            let encoder = JSONEncoder()
            let data = try encoder.encode(presets)
            UserDefaults.standard.set(data, forKey: userDefaultsKey)
        } catch {
            print("Failed to encode presets: \(error)")
        }
    }
    
    public func upsertPreset(_ preset: PresetSnapshot) {
        if let idx = presets.firstIndex(where: { $0.id == preset.id }) {
            presets[idx] = preset
        } else {
            presets.append(preset)
        }
        presets.sort(by: { $0.name.lowercased() < $1.name.lowercased() })
        savePresets()
    }
    
    public func deletePreset(presetId: String) {
        presets.removeAll(where: { $0.id == presetId })
        savePresets()
    }
    
    public func updatePresetName(presetId: String, newName: String) {
        if let idx = presets.firstIndex(where: { $0.id == presetId }) {
            let current = presets[idx]
            let updated = PresetSnapshot(
                id: current.id,
                name: newName,
                device: current.device,
                createdAtEpochMillis: current.createdAtEpochMillis,
                updatedAtEpochMillis: Int64(Date().timeIntervalSince1970 * 1000)
            )
            presets[idx] = updated
            presets.sort(by: { $0.name.lowercased() < $1.name.lowercased() })
            savePresets()
        }
    }
}
