import Foundation
import Combine

public struct DeviceInputState: Equatable, Identifiable {
    public let id: Int64
    public var name: String
    public var colorHex: String
    public var lenses: [LensInputState]
    
    public init(id: Int64, name: String, colorHex: String, lenses: [LensInputState]) {
        self.id = id
        self.name = name
        self.colorHex = colorHex
        self.lenses = lenses
    }
}

public struct LensInputState: Equatable, Identifiable {
    public let id: Int64
    public var nativeFocalLength: String
    public var selectedSensorValue: String
    public var manualSensorDescriptor: String
    public var fNumber: String
    
    public var usesManualSensor: Bool {
        return selectedSensorValue == MANUAL_INPUT_SENSOR_VALUE
    }
    
    public init(id: Int64, nativeFocalLength: String, selectedSensorValue: String, manualSensorDescriptor: String, fNumber: String) {
        self.id = id
        self.nativeFocalLength = nativeFocalLength
        self.selectedSensorValue = selectedSensorValue
        self.manualSensorDescriptor = manualSensorDescriptor
        self.fNumber = fNumber
    }
}

public struct PresetListItem: Equatable, Identifiable {
    public var id: String
    public let name: String
    public let deviceName: String
    public let lensCount: Int
    public let colorHex: String
    public let updatedAtEpochMillis: Int64
}

public enum PresetSheet: Equatable {
    case none
    case save
    case library
}

public struct SensorComparisonUiState: Equatable {
    public var devices: [DeviceInputState] = []
    public var availableSensors: [SensorSpec] = []
    public var availableDeviceColors: [String] = DEFAULT_DEVICE_COLORS
    public var selectedFocalLength: Double = 14.0
    public var comparisonResults: ComparisonResults? = nil
    public var focalLengths: [Double] = (14...260).map { Double($0) }
    public var presets: [PresetSnapshot] = []
    public var presetSheet: PresetSheet = .none
    public var presetNameInput: String = ""
    public var presetTargetDeviceId: Int64? = nil
    public var activePresetAssignments: [Int64: String] = [:]
    public var isPresetProcessing: Bool = false
    public var presetErrorMessage: String? = nil
    public var deviceFocusRequestId: Int64? = nil
    
    public var canAddDevice: Bool {
        return devices.count < MAX_DEVICES
    }
    public var hasResults: Bool {
        return comparisonResults != nil
    }
    public var isGenerateEnabled: Bool {
        return devices.contains { device in
            device.lenses.contains { lens in
                let flValid = Double(lens.nativeFocalLength).map { $0 > 0.0 } == true
                let fNumberValid = Double(lens.fNumber).map { $0 > 0.0 } == true
                let sensorValid = lens.usesManualSensor ? isValidManualSensorDescriptor(descriptor: lens.manualSensorDescriptor) : true
                return flValid && fNumberValid && sensorValid
            }
        }
    }
    public var presetTargetDevice: DeviceInputState? {
        guard let id = presetTargetDeviceId else { return nil }
        return devices.first(where: { $0.id == id })
    }
    public var presetListItems: [PresetListItem] {
        return presets.map { snapshot in
            let devName = snapshot.device.name.trimmingCharacters(in: .whitespacesAndNewlines)
            let color = snapshot.device.colorHex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
            let validatedColor = (color.count == 7 && color.hasPrefix("#")) ? color : DEFAULT_DEVICE_COLORS.first!
            return PresetListItem(
                id: snapshot.id,
                name: snapshot.name,
                deviceName: devName.isEmpty ? snapshot.name : devName,
                lensCount: snapshot.device.lenses.count,
                colorHex: validatedColor,
                updatedAtEpochMillis: snapshot.updatedAtEpochMillis
            )
        }
    }
    public var isPresetSaveEnabled: Bool {
        return !presetNameInput.trimmingCharacters(in: .whitespaces).isEmpty && (presetTargetDevice?.lenses.isEmpty == false)
    }
}

public class SensorComparisonViewModel: ObservableObject {
    @Published public var state = SensorComparisonUiState()
    @Published public var toastMessage: String? = nil
    
    public let presetRepository: PresetRepository
    public let customSensorRepository: CustomSensorRepository
    public let deviceInputRepository: DeviceInputRepository
    private let sensorDatabaseRepository: SensorDatabaseRepository
    private let generateComparisonUseCase: GenerateComparisonUseCase
    
    private var cancellables = Set<AnyCancellable>()
    private var nextDeviceId: Int64 = 1
    private var nextLensId: Int64 = 1
    private var hasRestoredState = false
    private let defaultManualSensorValue = "1/1.33"
    
    public init(
        presetRepository: PresetRepository = PresetRepository(),
        customSensorRepository: CustomSensorRepository = CustomSensorRepository(),
        deviceInputRepository: DeviceInputRepository = DeviceInputRepository(),
        sensorDatabaseRepository: SensorDatabaseRepository = SensorDatabaseRepository(),
        generateComparisonUseCase: GenerateComparisonUseCase = GenerateComparisonUseCase()
    ) {
        self.presetRepository = presetRepository
        self.customSensorRepository = customSensorRepository
        self.deviceInputRepository = deviceInputRepository
        self.sensorDatabaseRepository = sensorDatabaseRepository
        self.generateComparisonUseCase = generateComparisonUseCase
        
        setupBindings()
        loadInitialData()
    }
    
    private func setupBindings() {
        presetRepository.$presets
            .sink { [weak self] presets in
                guard let self = self else { return }
                var active = self.state.activePresetAssignments
                active = active.filter { _, presetId in
                    presets.contains(where: { $0.id == presetId })
                }
                self.state.presets = presets
                self.state.activePresetAssignments = active
            }
            .store(in: &cancellables)
            
        customSensorRepository.$sensors
            .sink { [weak self] custom in
                guard let self = self else { return }
                self.mergeAndPublishSensors(custom: custom)
            }
            .store(in: &cancellables)
    }
    
    private var baseSensors: [SensorSpec] = []
    
    private func loadInitialData() {
        switch sensorDatabaseRepository.loadSensors() {
        case .success(let loaded):
            self.baseSensors = loaded
        case .failure(let error):
            print("Failed to load sensor database: \(error)")
            postMessage(LocalizedStrings.errorFailedToLoadSensorDatabase)
        }
        
        mergeAndPublishSensors(custom: customSensorRepository.sensors)
    }
    
    private func mergeAndPublishSensors(custom: [CustomSensorEntry]) {
        let merged = mergeSensors(baseSensors: baseSensors, customSensors: custom)
        state.availableSensors = merged
        
        if !hasRestoredState {
            restoreState(sensors: merged)
        } else {
            refreshComparisonResults()
        }
    }
    
    private func restoreState(sensors: [SensorSpec]) {
        hasRestoredState = true
        
        let restoredDevices: [DeviceInputState]?
        switch deviceInputRepository.load() {
        case .success(let loaded):
            restoredDevices = loaded.map { buildDevicesFromSaved(saved: $0, sensors: sensors) }
        case .failure(let error):
            print("Failed to restore devices: \(error)")
            postMessage(LocalizedStrings.errorFailedToRestoreDeviceInputs)
            restoredDevices = nil
        }
        
        let devices = restoredDevices ?? createDefaultDevices(sensors: sensors)
        state.devices = devices
        state.presetTargetDeviceId = devices.first?.id
        refreshComparisonResults()
    }
    
    public func persistDevices() {
        guard hasRestoredState else { return }
        let snapshot = state.devices.map { device -> SavedDeviceInput in
            let lensesSnapshot = device.lenses.map { lens -> SavedLensInput in
                let manual = lens.selectedSensorValue == MANUAL_INPUT_SENSOR_VALUE ?
                    (lens.manualSensorDescriptor.isEmpty ? defaultManualSensorValue : lens.manualSensorDescriptor) : lens.manualSensorDescriptor
                return SavedLensInput(
                    nativeFocalLength: lens.nativeFocalLength,
                    selectedSensorValue: lens.selectedSensorValue,
                    manualSensorDescriptor: manual,
                    fNumber: lens.fNumber
                )
            }
            return SavedDeviceInput(
                name: device.name,
                colorHex: sanitizeColorHex(device.colorHex),
                lenses: lensesSnapshot
            )
        }
        switch deviceInputRepository.save(snapshot) {
        case .success:
            break
        case .failure(let error):
            print("Failed to persist devices: \(error)")
            postMessage(LocalizedStrings.errorFailedToSaveDeviceInputs)
        }
    }
    
    public func postMessage(_ message: String) {
        DispatchQueue.main.async {
            self.toastMessage = message
        }
    }
    
    public func addDevice() {
        if !state.canAddDevice { return }
        let color = DEFAULT_DEVICE_COLORS[state.devices.count % DEFAULT_DEVICE_COLORS.count]
        let newDevice = DeviceInputState(
            id: nextDeviceId,
            name: defaultDeviceName(index: Int(nextDeviceId)),
            colorHex: color,
            lenses: [newDefaultLens()]
        )
        nextDeviceId += 1
        
        state.devices.append(newDevice)
        if state.presetTargetDeviceId == nil {
            state.presetTargetDeviceId = newDevice.id
        }
        state.deviceFocusRequestId = newDevice.id
        state.presetErrorMessage = nil
        refreshComparisonResults()
        
        persistDevices()
    }
    
    public func removeDevice(deviceId: Int64) {
        state.devices.removeAll(where: { $0.id == deviceId })
        state.activePresetAssignments.removeValue(forKey: deviceId)
        
        if state.devices.isEmpty {
            state.presetTargetDeviceId = nil
        } else if state.presetTargetDeviceId == deviceId {
            state.presetTargetDeviceId = state.devices.first?.id
        }
        
        state.presetErrorMessage = nil
        refreshComparisonResults()
        
        persistDevices()
    }
    
    public func updateDeviceName(deviceId: Int64, name: String) {
        if let idx = state.devices.firstIndex(where: { $0.id == deviceId }) {
            state.devices[idx].name = name
            state.presetErrorMessage = nil
            refreshComparisonResults()
            persistDevices()
        }
    }
    
    public func updateDeviceColor(deviceId: Int64, colorHex: String) {
        if let idx = state.devices.firstIndex(where: { $0.id == deviceId }) {
            state.devices[idx].colorHex = colorHex
            state.presetErrorMessage = nil
            refreshComparisonResults()
            persistDevices()
        }
    }
    
    public func addLens(deviceId: Int64) {
        if let idx = state.devices.firstIndex(where: { $0.id == deviceId }) {
            if state.devices[idx].lenses.count >= MAX_LENSES_PER_DEVICE { return }
            state.devices[idx].lenses.append(newDefaultLens())
            state.presetErrorMessage = nil
            refreshComparisonResults()
            persistDevices()
        }
    }
    
    public func removeLens(deviceId: Int64, lensId: Int64) {
        if let idx = state.devices.firstIndex(where: { $0.id == deviceId }) {
            state.devices[idx].lenses.removeAll(where: { $0.id == lensId })
            if state.devices[idx].lenses.isEmpty {
                state.devices[idx].lenses.append(newDefaultLens())
            }
            state.presetErrorMessage = nil
            refreshComparisonResults()
            persistDevices()
        }
    }
    
    public func openPresetSave() {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let targetId = state.presetTargetDeviceId.flatMap { id in state.devices.contains(where: { $0.id == id }) ? id : nil } ?? state.devices.first?.id
        let targetName = targetId.flatMap { id in state.devices.first(where: { $0.id == id })?.name }
        let defaultName = buildDefaultPresetName(deviceName: targetName, nowEpochMillis: now)
        
        state.presetSheet = .save
        state.presetErrorMessage = nil
        state.presetTargetDeviceId = targetId
        state.presetNameInput = state.presetNameInput.isEmpty ? defaultName : state.presetNameInput
    }
    
    public func openPresetLibrary() {
        let targetId = state.presetTargetDeviceId.flatMap { id in state.devices.contains(where: { $0.id == id }) ? id : nil } ?? state.devices.first?.id
        state.presetSheet = .library
        state.presetErrorMessage = nil
        state.presetTargetDeviceId = targetId
    }
    
    public func closePresetSheet() {
        state.presetSheet = .none
        state.presetNameInput = ""
        state.presetErrorMessage = nil
    }
    
    public func consumeDeviceFocusRequest() {
        state.deviceFocusRequestId = nil
    }
    
    public func updatePresetNameInput(_ newValue: String) {
        state.presetNameInput = String(newValue.prefix(40))
    }
    
    public func updatePresetTargetDevice(_ deviceId: Int64?) {
        let resolvedId = deviceId.flatMap { id in state.devices.contains(where: { $0.id == id }) ? id : nil } ?? state.devices.first?.id
        state.presetTargetDeviceId = resolvedId
        state.presetErrorMessage = nil
    }
    
    public func savePreset() {
        let trimmedName = state.presetNameInput.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmedName.isEmpty {
            state.presetErrorMessage = LocalizedStrings.errorPresetNameRequired
            return
        }
        guard let targetDevice = state.presetTargetDevice else {
            state.presetErrorMessage = LocalizedStrings.errorPresetTargetRequired
            return
        }
        let deviceSnapshot = targetDevice.toSnapshot()
        if deviceSnapshot.lenses.isEmpty {
            state.presetErrorMessage = LocalizedStrings.errorPresetNoLenses
            return
        }
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let snapshot = PresetSnapshot(
            id: UUID().uuidString,
            name: trimmedName,
            device: deviceSnapshot,
            createdAtEpochMillis: now,
            updatedAtEpochMillis: now
        )
        
        state.isPresetProcessing = true
        state.presetErrorMessage = nil
        
        presetRepository.upsertPreset(snapshot)
        
        state.isPresetProcessing = false
        if state.devices.contains(where: { $0.id == targetDevice.id }) {
            state.activePresetAssignments[targetDevice.id] = snapshot.id
        }
        state.presetSheet = .none
        state.presetNameInput = ""
        
        postMessage(String(format: LocalizedStrings.messagePresetSaved, snapshot.name))
    }
    
    public func loadPreset(_ presetId: String) {
        guard let snapshot = state.presets.first(where: { $0.id == presetId }) else { return }
        if state.devices.count >= MAX_DEVICES {
            state.presetErrorMessage = String(format: LocalizedStrings.errorMaxDevicesReached, MAX_DEVICES)
            return
        }
        
        let newDeviceId = nextDeviceId
        nextDeviceId += 1
        let fallbackColor = DEFAULT_DEVICE_COLORS[state.devices.count % DEFAULT_DEVICE_COLORS.count]
        let newDevice = createDeviceFromPreset(snapshot: snapshot, sensors: state.availableSensors, deviceId: newDeviceId, fallbackColor: fallbackColor)
        
        state.devices.append(newDevice)
        state.activePresetAssignments[newDeviceId] = presetId
        state.presetSheet = .none
        state.presetNameInput = ""
        state.presetErrorMessage = nil
        state.presetTargetDeviceId = newDeviceId
        state.deviceFocusRequestId = newDeviceId
        refreshComparisonResults()
        
        persistDevices()
        postMessage(String(format: LocalizedStrings.messageDeviceAdded, newDevice.name))
    }
    
    public func overwriteTargetDeviceFromPreset(presetId: String) {
        guard let targetDeviceId = state.presetTargetDeviceId else {
            state.presetErrorMessage = LocalizedStrings.errorOverwriteTargetRequired
            return
        }
        guard let snapshot = state.presets.first(where: { $0.id == presetId }) else { return }
        guard let targetDevice = state.devices.first(where: { $0.id == targetDeviceId }) else {
            state.presetErrorMessage = LocalizedStrings.errorOverwriteTargetNotFound
            return
        }
        
        let overwritten = createDeviceFromPreset(
            snapshot: snapshot,
            sensors: state.availableSensors,
            deviceId: targetDeviceId,
            fallbackColor: targetDevice.colorHex
        )
        
        if let idx = state.devices.firstIndex(where: { $0.id == targetDeviceId }) {
            state.devices[idx] = overwritten
            state.presetSheet = .none
            state.presetErrorMessage = nil
            state.deviceFocusRequestId = targetDeviceId
            state.activePresetAssignments[targetDeviceId] = presetId
            refreshComparisonResults()
            
            persistDevices()
            postMessage(String(format: LocalizedStrings.messageDeviceOverwritten, overwritten.name))
        }
    }
    
    public func deletePreset(_ presetId: String) {
        presetRepository.deletePreset(presetId: presetId)
        state.activePresetAssignments = state.activePresetAssignments.filter { $1 != presetId }
        postMessage(LocalizedStrings.messagePresetDeleted)
    }
    
    public func renamePreset(_ presetId: String, newName: String) {
        let trimmedName = newName.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmedName.isEmpty {
            state.presetErrorMessage = LocalizedStrings.errorNewPresetNameRequired
            return
        }
        presetRepository.updatePresetName(presetId: presetId, newName: String(trimmedName.prefix(40)))
        postMessage(LocalizedStrings.messagePresetRenamed)
    }
    
    public func updateLensFocalLength(deviceId: Int64, lensId: Int64, value: String) {
        updateLens(deviceId: deviceId, lensId: lensId) { lens in
            var updated = lens
            updated.nativeFocalLength = value
            return updated
        }
    }
    
    public func updateLensFNumber(deviceId: Int64, lensId: Int64, value: String) {
        updateLens(deviceId: deviceId, lensId: lensId) { lens in
            var updated = lens
            updated.fNumber = value
            return updated
        }
    }
    
    public func updateLensSensorSelection(deviceId: Int64, lensId: Int64, newValue: String) {
        updateLens(deviceId: deviceId, lensId: lensId) { [weak self] lens in
            var updated = lens
            updated.selectedSensorValue = newValue
            if newValue == MANUAL_INPUT_SENSOR_VALUE {
                if updated.manualSensorDescriptor.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    updated.manualSensorDescriptor = self?.defaultManualSensorValue ?? "1/1.33"
                }
            }
            return updated
        }
    }
    
    public func updateLensManualDescriptor(deviceId: Int64, lensId: Int64, descriptor: String) {
        updateLens(deviceId: deviceId, lensId: lensId) { lens in
            var updated = lens
            updated.manualSensorDescriptor = descriptor
            return updated
        }
    }
    
    public func updateFocalLength(focalLength: Double) {
        let coerced = max(state.focalLengths.first ?? 14.0, min(focalLength, state.focalLengths.last ?? 260.0))
        if coerced == state.selectedFocalLength { return }
        state.selectedFocalLength = coerced
    }
    
    public func generateComparison() {
        refreshComparisonResults()
    }

    private func refreshComparisonResults() {
        if !state.isGenerateEnabled {
            state.comparisonResults = nil
            return
        }
        
        let output = generateComparisonUseCase.generate(
            devices: state.devices,
            availableSensors: state.availableSensors,
            selectedFocalLength: state.selectedFocalLength,
            defaultFocalLengths: (14...260).map { Double($0) },
            fallbackDeviceName: { [weak self] index in
                self?.defaultDeviceName(index: index) ?? "Device \(index)"
            }
        )
        
        state.comparisonResults = output.results
        state.selectedFocalLength = output.selectedFocalLength
        state.focalLengths = output.focalLengths
    }
    
    private func updateLens(deviceId: Int64, lensId: Int64, transform: (LensInputState) -> LensInputState) {
        if let devIdx = state.devices.firstIndex(where: { $0.id == deviceId }) {
            if let lensIdx = state.devices[devIdx].lenses.firstIndex(where: { $0.id == lensId }) {
                state.devices[devIdx].lenses[lensIdx] = transform(state.devices[devIdx].lenses[lensIdx])
                state.presetErrorMessage = nil
                refreshComparisonResults()
                persistDevices()
            }
        }
    }
    
    private func buildDevicesFromSaved(
        saved: [SavedDeviceInput],
        sensors: [SensorSpec]
    ) -> [DeviceInputState] {
        nextDeviceId = 1
        nextLensId = 1
        var sensorLookup: [String: SensorSpec] = [:]
        for sensor in sensors {
            sensorLookup[sensor.value] = sensor
        }

        return saved.prefix(MAX_DEVICES).enumerated().map { deviceIndex, device in
            let deviceId = nextDeviceId
            nextDeviceId += 1
            let fallbackColor = DEFAULT_DEVICE_COLORS[deviceIndex % DEFAULT_DEVICE_COLORS.count]
            let lenses = device.lenses.prefix(MAX_LENSES_PER_DEVICE).map { lens -> LensInputState in
                let selection = resolveSensorSelection(
                    rawValue: !lens.selectedSensorValue.isEmpty ? lens.selectedSensorValue : MANUAL_INPUT_SENSOR_VALUE,
                    manualDescriptor: lens.manualSensorDescriptor,
                    sensorLookup: sensorLookup
                )
                let lensState = LensInputState(
                    id: nextLensId,
                    nativeFocalLength: lens.nativeFocalLength.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "24" : lens.nativeFocalLength,
                    selectedSensorValue: selection.value,
                    manualSensorDescriptor: selection.manualDescriptor ?? "",
                    fNumber: lens.fNumber.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "1.8" : lens.fNumber
                )
                nextLensId += 1
                return lensState
            }
            let finalLenses = lenses.isEmpty ? [newDefaultLens()] : Array(lenses)
            return DeviceInputState(
                id: deviceId,
                name: device.name.isEmpty ? defaultDeviceName(index: deviceIndex + 1) : device.name,
                colorHex: sanitizeColorHex(device.colorHex, fallback: fallbackColor),
                lenses: finalLenses
            )
        }
    }
    
    private func buildDefaultPresetName(deviceName: String?, nowEpochMillis: Int64) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy/MM/dd HH:mm"
        let timestamp = formatter.string(from: Date(timeIntervalSince1970: Double(nowEpochMillis) / 1000))
        let base = deviceName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? defaultDeviceBaseName()
        let combined = base.isEmpty ? "\(defaultDeviceBaseName()) \(timestamp)" : "\(base) \(timestamp)"
        return String(combined.prefix(40))
    }
    
    private func createDeviceFromPreset(
        snapshot: PresetSnapshot,
        sensors: [SensorSpec],
        deviceId: Int64,
        fallbackColor: String
    ) -> DeviceInputState {
        var sensorLookup: [String: SensorSpec] = [:]
        for sensor in sensors {
            sensorLookup[sensor.value] = sensor
        }
        
        let deviceSnapshot = snapshot.device
        var lenses: [LensInputState] = []
        for lensSnapshot in deviceSnapshot.lenses.prefix(MAX_LENSES_PER_DEVICE) {
            let resolved = resolveSensorSelection(
                rawValue: lensSnapshot.selectedSensorValue,
                manualDescriptor: lensSnapshot.manualSensorDescriptor,
                sensorLookup: sensorLookup
            )
            let manual = resolved.value == MANUAL_INPUT_SENSOR_VALUE ?
                (resolved.manualDescriptor ?? (lensSnapshot.manualSensorDescriptor.isEmpty ? defaultManualSensorValue : lensSnapshot.manualSensorDescriptor)) : ""
            
            lenses.append(LensInputState(
                id: nextLensId,
                nativeFocalLength: lensSnapshot.nativeFocalLength.isEmpty ? "24" : lensSnapshot.nativeFocalLength,
                selectedSensorValue: resolved.value,
                manualSensorDescriptor: manual,
                fNumber: lensSnapshot.fNumber.isEmpty ? "1.8" : lensSnapshot.fNumber
            ))
            nextLensId += 1
        }
        
        let sanitizedName = deviceSnapshot.name.isEmpty ? (snapshot.name.isEmpty ? LocalizedStrings.labelPresetDeviceDefaultName : snapshot.name) : deviceSnapshot.name
        let sanitizedColor = sanitizeColorHex(deviceSnapshot.colorHex, fallback: fallbackColor)
        
        return DeviceInputState(
            id: deviceId,
            name: sanitizedName,
            colorHex: sanitizedColor,
            lenses: lenses.isEmpty ? [newDefaultLens()] : lenses
        )
    }
    
    private func newDefaultLens() -> LensInputState {
        let lens = LensInputState(
            id: nextLensId,
            nativeFocalLength: "24",
            selectedSensorValue: MANUAL_INPUT_SENSOR_VALUE,
            manualSensorDescriptor: defaultManualSensorValue,
            fNumber: "1.8"
        )
        nextLensId += 1
        return lens
    }
    
    private func mergeSensors(
        baseSensors: [SensorSpec],
        customSensors: [CustomSensorEntry]
    ) -> [SensorSpec] {
        let manual = baseSensors.first(where: { $0.isManual })
        let baseNonManual = baseSensors.filter { !$0.isManual }
        let customSpecs = customSensors.map { $0.toSensorSpec() }.sorted(by: { $0.name.lowercased() < $1.name.lowercased() })
        
        var result: [SensorSpec] = []
        if let m = manual {
            result.append(m)
        }
        result.append(contentsOf: baseNonManual)
        result.append(contentsOf: customSpecs)
        return result
    }
    
    private func createDefaultDevices(sensors: [SensorSpec]) -> [DeviceInputState] {
        let presets = [
            DevicePreset(
                name: defaultDeviceName(index: 1),
                lenses: [
                    LensPreset(focalLength: 14.0, sensorName: "1/2.76", fNumber: 2.2),
                    LensPreset(focalLength: 23.0, sensorName: "Sony LYT-900 (IMX06A)", fNumber: 1.63),
                    LensPreset(focalLength: 70.0, sensorName: "1/2.51", fNumber: 1.8),
                    LensPreset(focalLength: 100.0, sensorName: "1/1.4", fNumber: 2.6)
                ]
            ),
            DevicePreset(
                name: defaultDeviceName(index: 2),
                lenses: [
                    LensPreset(focalLength: 15.0, sensorName: "1/2.75", fNumber: 2.0),
                    LensPreset(focalLength: 23.0, sensorName: "Sony IMX989", fNumber: 1.8),
                    LensPreset(focalLength: 70.0, sensorName: "1/1.56", fNumber: 2.1),
                    LensPreset(focalLength: 135.0, sensorName: "1/1.95", fNumber: 3.2)
                ]
            ),
            DevicePreset(
                name: defaultDeviceName(index: 3),
                lenses: [
                    LensPreset(focalLength: 14.0, sensorName: "Sony IMX707", fNumber: 2.0),
                    LensPreset(focalLength: 35.0, sensorName: "Sony IMX803", fNumber: 1.69),
                    LensPreset(focalLength: 85.0, sensorName: "Samsung GN2 (S5KGN2)", fNumber: 2.27)
                ]
            )
        ]
        
        var sensorByName: [String: SensorSpec] = [:]
        for sensor in sensors {
            sensorByName[sensor.name] = sensor
        }
        
        return presets.prefix(MAX_DEVICES).enumerated().map { index, preset in
            let deviceId = nextDeviceId
            nextDeviceId += 1
            let lenses = preset.lenses.prefix(MAX_LENSES_PER_DEVICE).map { lensPreset -> LensInputState in
                let sensor = sensorByName[lensPreset.sensorName]
                let isManual = sensor == nil || sensor!.isManual
                let lens = LensInputState(
                    id: nextLensId,
                    nativeFocalLength: String(format: "%g", lensPreset.focalLength),
                    selectedSensorValue: isManual ? MANUAL_INPUT_SENSOR_VALUE : sensor!.value,
                    manualSensorDescriptor: isManual ? lensPreset.sensorName : "",
                    fNumber: String(format: "%g", lensPreset.fNumber)
                )
                nextLensId += 1
                return lens
            }
            let color = DEFAULT_DEVICE_COLORS[index % DEFAULT_DEVICE_COLORS.count]
            return DeviceInputState(
                id: deviceId,
                name: preset.name,
                colorHex: sanitizeColorHex(color),
                lenses: lenses.isEmpty ? [newDefaultLens()] : lenses
            )
        }
    }
    
    private struct DevicePreset {
        let name: String
        let lenses: [LensPreset]
    }
    
    private struct LensPreset {
        let focalLength: Double
        let sensorName: String
        let fNumber: Double
    }
    
    private func defaultDeviceName(index: Int) -> String {
        return String(format: LocalizedStrings.labelDeviceNumberedName, index)
    }
    
    private func defaultDeviceBaseName() -> String {
        return LocalizedStrings.labelDeviceDefaultName
    }
    
    private func sanitizeColorHex(_ raw: String?, fallback: String = DEFAULT_DEVICE_COLORS.first!) -> String {
        guard let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() else {
            return fallback.uppercased()
        }
        let hexRegex = try! NSRegularExpression(pattern: "^#[0-9A-F]{6}$")
        let range = NSRange(location: 0, length: trimmed.utf16.count)
        if hexRegex.firstMatch(in: trimmed, options: [], range: range) != nil {
            return trimmed
        }
        return fallback.uppercased()
    }
    
    public func addCustomSensor(name: String, megapixels: Double, pixelSizeUm: Double, binningType: String) -> Result<Void, Error> {
        let nameTrimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        if nameTrimmed.isEmpty {
            return .failure(NSError(domain: "ViewModel", code: 400, userInfo: [NSLocalizedDescriptionKey: "Sensor name is empty"]))
        }
        
        let duplicate = state.availableSensors.contains { $0.name.lowercased() == nameTrimmed.lowercased() }
        if duplicate {
            return .failure(NSError(domain: "ViewModel", code: 400, userInfo: [NSLocalizedDescriptionKey: LocalizedStrings.errorDuplicateSensorName]))
        }
        
        let newEntry = CustomSensorEntry(
            id: UUID().uuidString,
            name: nameTrimmed,
            megapixels: megapixels,
            pixelSizeUm: pixelSizeUm,
            binningType: binningType
        )
        customSensorRepository.upsertSensor(newEntry)
        return .success(())
    }
    
    public func deleteCustomSensor(id: String) {
        customSensorRepository.deleteSensor(id: id)
    }
}

extension DeviceInputState {
    public func toSnapshot() -> PresetDeviceSnapshot {
        let lensesSnapshot = lenses.prefix(MAX_LENSES_PER_DEVICE).map { lens -> PresetLensSnapshot in
            let sanitizedFocal = lens.nativeFocalLength.trimmingCharacters(in: .whitespacesAndNewlines)
            let sanitizedFNumber = lens.fNumber.trimmingCharacters(in: .whitespacesAndNewlines)
            let manual = lens.usesManualSensor ? lens.manualSensorDescriptor.trimmingCharacters(in: .whitespacesAndNewlines) : ""
            return PresetLensSnapshot(
                nativeFocalLength: sanitizedFocal.isEmpty ? "24" : sanitizedFocal,
                selectedSensorValue: lens.selectedSensorValue,
                manualSensorDescriptor: manual.isEmpty ? "1/1.33" : manual,
                fNumber: sanitizedFNumber.isEmpty ? "1.8" : sanitizedFNumber
            )
        }
        return PresetDeviceSnapshot(
            name: name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? LocalizedStrings.labelDeviceDefaultName : name,
            colorHex: colorHex,
            lenses: lensesSnapshot
        )
    }
}

internal func resolveSensorSelection(
    rawValue: String,
    manualDescriptor: String,
    sensorLookup: [String: SensorSpec]
) -> ResolvedSensorSelection {
    let trimmed = rawValue.trimmingCharacters(in: .whitespacesAndNewlines)
    if trimmed == MANUAL_INPUT_SENSOR_VALUE {
        let normalizedManual = manualDescriptor.trimmingCharacters(in: .whitespacesAndNewlines)
        return ResolvedSensorSelection(value: MANUAL_INPUT_SENSOR_VALUE, manualDescriptor: normalizedManual.isEmpty ? nil : normalizedManual)
    }
    
    if let spec = sensorLookup[trimmed] {
        return ResolvedSensorSelection(value: spec.value, manualDescriptor: nil)
    }
    
    if let matchedByName = sensorLookup.values.first(where: { $0.name.lowercased() == trimmed.lowercased() }) {
        return ResolvedSensorSelection(value: matchedByName.value, manualDescriptor: nil)
    }
    
    let fallbackDescriptor = !manualDescriptor.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?
        manualDescriptor.trimmingCharacters(in: .whitespacesAndNewlines) : (!trimmed.isEmpty ? trimmed : "1/1.33")
    return ResolvedSensorSelection(value: MANUAL_INPUT_SENSOR_VALUE, manualDescriptor: fallbackDescriptor)
}

internal struct ResolvedSensorSelection {
    let value: String
    let manualDescriptor: String?
}
