import SwiftUI

public struct SensorPickerSheetView: View {
    @ObservedObject var viewModel: SensorComparisonViewModel
    let onSelected: (String) -> Void
    @Environment(\.dismiss) var dismiss
    
    @State private var searchQuery = ""
    @State private var isShowingAddCustomSensor = false
    
    // Add custom sensor state
    @State private var customName = ""
    @State private var customMegapixels = "50"
    @State private var customPixelSize = "1.0"
    @State private var customBinning = "None"
    @State private var errorText: String? = nil
    
    var filteredSensors: [SensorSpec] {
        let query = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        if query.isEmpty {
            return viewModel.state.availableSensors
        }
        return viewModel.state.availableSensors.filter {
            $0.name.localizedCaseInsensitiveContains(query) ||
            $0.value.localizedCaseInsensitiveContains(query)
        }
    }
    
    public init(viewModel: SensorComparisonViewModel, onSelected: @escaping (String) -> Void) {
        self.viewModel = viewModel
        self.onSelected = onSelected
    }
    
    public var body: some View {
        NavigationView {
            VStack {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField(LocalizedStrings.placeholderSearchSensor, text: $searchQuery)
                        .textFieldStyle(PlainTextFieldStyle())
                    if !searchQuery.isEmpty {
                        Button(action: { searchQuery = "" }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding(10)
                .background(Color(.secondarySystemBackground))
                .cornerRadius(10)
                .padding(.horizontal)
                .padding(.top)
                
                List {
                    Button(action: {
                        isShowingAddCustomSensor = true
                    }) {
                        HStack {
                            Image(systemName: "plus.circle.fill")
                                .foregroundColor(.accentColor)
                            Text(LocalizedStrings.titleAddSensor)
                                .fontWeight(.bold)
                        }
                    }
                    
                    if filteredSensors.isEmpty {
                        Text(LocalizedStrings.textNoMatchingSensors)
                            .foregroundColor(.secondary)
                            .italic()
                            .padding()
                    } else {
                        ForEach(filteredSensors, id: \.value) { sensor in
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(sensor.name)
                                        .font(.body)
                                        .fontWeight(.semibold)
                                        .foregroundColor(.primary)
                                    
                                    HStack(spacing: 8) {
                                        if sensor.isManual {
                                            Text(LocalizedStrings.labelManualInput)
                                                .font(.caption)
                                                .foregroundColor(.orange)
                                        } else {
                                            Text(sensor.value)
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                            
                                            Text("\(String(format: "%g", sensor.megapixels))MP")
                                                .font(.caption)
                                                .foregroundColor(.blue)
                                            
                                            Text("\(String(format: "%g", sensor.pixelSizeUm))µm")
                                                .font(.caption)
                                                .foregroundColor(.purple)
                                        }
                                    }
                                }
                                
                                Spacer()
                                
                                // Detect if custom sensor
                                if sensor.source == .DATABASE && sensor.value != MANUAL_INPUT_SENSOR_VALUE && !viewModel.presetRepository.presets.contains(where: { _ in false }) {
                                    let isBase = viewModel.state.availableSensors.contains { base in
                                        base.value == sensor.value && base.source == .DATABASE && base.isManual == false && !sensor.value.contains("-") && (sensor.value.starts(with: "1/") || sensor.value.contains("Sony") || sensor.value.contains("Samsung") || sensor.value.contains("OmniVision"))
                                    }
                                    // Custom sensors have UUID value keys
                                    if !isBase && sensor.value.count > 8 {
                                        Button(action: {
                                            viewModel.deleteCustomSensor(id: sensor.value)
                                        }) {
                                            Image(systemName: "trash")
                                                .foregroundColor(.red)
                                        }
                                        .buttonStyle(PlainButtonStyle())
                                    }
                                }
                            }
                            .contentShape(Rectangle())
                            .onTapGesture {
                                onSelected(sensor.value)
                                dismiss()
                            }
                        }
                    }
                }
            }
            .navigationTitle(LocalizedStrings.titleSensorPicker)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(LocalizedStrings.actionClose) {
                        dismiss()
                    }
                }
            }
            .sheet(isPresented: $isShowingAddCustomSensor) {
                customSensorEditorSheet
            }
        }
    }
    
    private var customSensorEditorSheet: some View {
        NavigationView {
            Form {
                Section(header: Text(LocalizedStrings.helperCustomSensorEditor)) {
                    TextField(LocalizedStrings.labelSensorName, text: $customName)
                    TextField(LocalizedStrings.labelMegapixels, text: $customMegapixels)
                        .keyboardType(.decimalPad)
                    TextField(LocalizedStrings.labelPixelSizeUm, text: $customPixelSize)
                        .keyboardType(.decimalPad)
                    
                    Picker(LocalizedStrings.metricBinningCharacteristic, selection: $customBinning) {
                        ForEach(["None", "Quad Bayer (4-in-1)", "Nona Bayer (9-in-1)", "Hexadeca Bayer (16-in-1)"], id: \.self) { bin in
                            Text(bin).tag(bin)
                        }
                    }
                }
                
                if let errorText = errorText {
                    Section {
                        Text(errorText)
                            .foregroundColor(.red)
                            .font(.caption)
                    }
                }
            }
            .navigationTitle(LocalizedStrings.titleAddSensor)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(LocalizedStrings.actionCancel) {
                        isShowingAddCustomSensor = false
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(LocalizedStrings.actionSave) {
                        saveCustomSensor()
                    }
                }
            }
        }
    }
    
    private func saveCustomSensor() {
        guard let megapixels = Double(customMegapixels), megapixels > 0.0 else {
            errorText = "画素数を正しく入力してください"
            return
        }
        guard let pixelSize = Double(customPixelSize), pixelSize > 0.0 else {
            errorText = "ピクセルサイズを正しく入力してください"
            return
        }
        
        let bType: String
        switch customBinning {
        case "Quad Bayer (4-in-1)": bType = "QUAD_BAYER"
        case "Nona Bayer (9-in-1)": bType = "NONA_BAYER"
        case "Hexadeca Bayer (16-in-1)": bType = "HEXADECA_BAYER"
        default: bType = "NONE"
        }
        
        switch viewModel.addCustomSensor(name: customName, megapixels: megapixels, pixelSizeUm: pixelSize, binningType: bType) {
        case .success:
            isShowingAddCustomSensor = false
            customName = ""
            errorText = nil
        case .failure(let error):
            errorText = error.localizedDescription
        }
    }
}
