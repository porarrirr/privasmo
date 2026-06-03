import SwiftUI

public struct GraphSettingsScreen: View {
    @Environment(\.dismiss) var dismiss

    @AppStorage("app_language_override") var appLanguage: String = ""

    @StateObject private var repository = GraphSettingsRepository()
    @StateObject private var sensorRepository = CustomSensorRepository()

    @State private var lineWidth: Float = GraphSettings.defaultLineWidth
    @State private var exportAspectWidth = "4"
    @State private var exportAspectHeight = "3"

    @State private var isShowingAddDialog = false
    @State private var editingSensor: CustomSensorEntry? = nil
    @State private var isShowingDeleteAlert = false
    @State private var deleteTarget: CustomSensorEntry? = nil

    public init() {}

    public var body: some View {
        NavigationView {
            Form {
                // Language Selection Section
                Section(header: Text(LocalizedStrings.labelLanguage)) {
                    Picker(LocalizedStrings.labelLanguage, selection: $appLanguage) {
                        Text(LocalizedStrings.labelLanguageAuto).tag("")
                        Text("日本語").tag("ja")
                        Text("English").tag("en")
                        Text("简体中文").tag("zh-Hans")
                        Text("繁體中文").tag("zh-Hant")
                    }
                }

                // Line Width Section
                Section(header: Text(LocalizedStrings.labelChartLineWidth)) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(String(format: "%.1f", lineWidth))
                            .font(.subheadline.bold())

                        Slider(
                            value: $lineWidth,
                            in: GraphSettings.minLineWidth...GraphSettings.maxLineWidth,
                            step: 0.5
                        ) { _ in
                            repository.setLineWidth(lineWidth)
                        }
                    }
                    .padding(.vertical, 4)
                }

                // Export Ratio Section
                Section(header: Text(LocalizedStrings.labelExportImageAspectRatio)) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(String(format: LocalizedStrings.helperExportImageAspectRatioRange, GraphSettings.minExportAspectComponent, GraphSettings.maxExportAspectComponent))
                            .font(.caption)
                            .foregroundColor(.secondary)

                        Text(String(format: LocalizedStrings.labelCurrentExportAspectRatio, repository.settings.exportAspectWidth, repository.settings.exportAspectHeight))
                            .font(.subheadline)

                        HStack(spacing: 12) {
                            Button("4:3") {
                                exportAspectWidth = "4"
                                exportAspectHeight = "3"
                                repository.setExportAspectRatio(width: 4, height: 3)
                            }
                            .buttonStyle(.bordered)

                            Button("1:1") {
                                exportAspectWidth = "1"
                                exportAspectHeight = "1"
                                repository.setExportAspectRatio(width: 1, height: 1)
                            }
                            .buttonStyle(.bordered)

                            Button("16:9") {
                                exportAspectWidth = "16"
                                exportAspectHeight = "9"
                                repository.setExportAspectRatio(width: 16, height: 9)
                            }
                            .buttonStyle(.bordered)
                        }
                        .padding(.vertical, 4)

                        HStack {
                            TextField(LocalizedStrings.labelAspectRatioWidth, text: $exportAspectWidth)
                                .keyboardType(.numberPad)
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                                .frame(width: 80)

                            Text(":")

                            TextField(LocalizedStrings.labelAspectRatioHeight, text: $exportAspectHeight)
                                .keyboardType(.numberPad)
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                                .frame(width: 80)

                            Spacer()

                            Button(LocalizedStrings.actionApplyExportImageAspectRatio) {
                                if let w = Int(exportAspectWidth), let h = Int(exportAspectHeight) {
                                    repository.setExportAspectRatio(width: w, height: h)
                                }
                            }
                            .buttonStyle(.borderedProminent)
                            .disabled(Int(exportAspectWidth) == nil || Int(exportAspectHeight) == nil)
                        }
                    }
                }

                // Custom Sensors Section
                Section(header: Text(LocalizedStrings.labelCustomSensorList)) {
                    Button(action: {
                        editingSensor = nil
                        isShowingAddDialog = true
                    }) {
                        HStack {
                            Image(systemName: "plus.circle.fill")
                            Text(LocalizedStrings.actionAdd)
                        }
                    }

                    if sensorRepository.sensors.isEmpty {
                        Text(LocalizedStrings.textNoCustomSensors)
                            .foregroundColor(.secondary)
                            .italic()
                    } else {
                        ForEach(sensorRepository.sensors) { entry in
                            VStack(alignment: .leading, spacing: 6) {
                                Text(entry.name)
                                    .fontWeight(.bold)

                                HStack {
                                    Text(String(format: "%.2f MP / %.2f µm / %@", entry.megapixels, entry.pixelSizeUm, entry.binningType))
                                        .font(.caption)
                                        .foregroundColor(.secondary)

                                    Spacer()

                                    HStack(spacing: 16) {
                                        Button(action: {
                                            editingSensor = entry
                                            isShowingAddDialog = true
                                        }) {
                                            Image(systemName: "pencil")
                                                .foregroundColor(.accentColor)
                                        }
                                        .buttonStyle(PlainButtonStyle())

                                        Button(action: {
                                            deleteTarget = entry
                                            isShowingDeleteAlert = true
                                        }) {
                                            Image(systemName: "trash")
                                                .foregroundColor(.red)
                                        }
                                        .buttonStyle(PlainButtonStyle())
                                    }
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }

                // Reset Button
                Section {
                    Button(LocalizedStrings.actionResetDefault) {
                        repository.reset()
                        lineWidth = GraphSettings.defaultLineWidth
                        exportAspectWidth = "4"
                        exportAspectHeight = "3"
                    }
                    .foregroundColor(.red)
                }
            }
            .navigationTitle(LocalizedStrings.settingsTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(LocalizedStrings.actionBack) {
                        dismiss()
                    }
                }
            }
            .onAppear {
                lineWidth = repository.settings.lineWidth
                exportAspectWidth = String(repository.settings.exportAspectWidth)
                exportAspectHeight = String(repository.settings.exportAspectHeight)
            }
            .sheet(isPresented: $isShowingAddDialog) {
                CustomSensorEditorDialog(initial: editingSensor, sensorRepository: sensorRepository) {
                    isShowingAddDialog = false
                }
            }
            .alert(LocalizedStrings.dialogDeleteSensorTitle, isPresented: $isShowingDeleteAlert) {
                Button(LocalizedStrings.actionDelete, role: .destructive) {
                    if let target = deleteTarget {
                        sensorRepository.deleteSensor(id: target.id)
                    }
                }
                Button(LocalizedStrings.actionCancel, role: .cancel) {}
            } message: {
                if let target = deleteTarget {
                    Text(String(format: LocalizedStrings.dialogDeleteSensorMessage, target.name))
                }
            }
        }
    }
}

struct CustomSensorEditorDialog: View {
    let initial: CustomSensorEntry?
    @ObservedObject var sensorRepository: CustomSensorRepository
    let onDismiss: () -> Void

    @State private var name = ""
    @State private var megapixels = "50"
    @State private var pixelSize = "1.0"
    @State private var binning = "None"
    @State private var errorText: String? = nil

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text(LocalizedStrings.helperCustomSensorEditor)) {
                    TextField(LocalizedStrings.labelSensorName, text: $name)
                    TextField(LocalizedStrings.labelMegapixels, text: $megapixels)
                        .keyboardType(.decimalPad)
                    TextField(LocalizedStrings.labelPixelSizeUm, text: $pixelSize)
                        .keyboardType(.decimalPad)

                    Picker(LocalizedStrings.metricBinningCharacteristic, selection: $binning) {
                        ForEach(["None", "Quad Bayer (2x2)", "Nona (3x3)", "16-cell (4x4)", "Unknown"], id: \.self) { bin in
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
            .navigationTitle(initial == nil ? LocalizedStrings.titleAddSensor : LocalizedStrings.titleEditSensor)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(LocalizedStrings.actionCancel) {
                        onDismiss()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(LocalizedStrings.actionSave) {
                        save()
                    }
                }
            }
            .onAppear {
                if let initial = initial {
                    name = initial.name
                    megapixels = String(format: "%g", initial.megapixels)
                    pixelSize = String(format: "%g", initial.pixelSizeUm)
                    binning = normalizeBinning(raw: initial.binningType)
                }
            }
        }
    }

    private func save() {
        let nameTrimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        if nameTrimmed.isEmpty {
            errorText = LocalizedStrings.errorNameRequired
            return
        }

        let dup = sensorRepository.sensors.contains {
            $0.name.lowercased() == nameTrimmed.lowercased() && $0.id != (initial?.id ?? "")
        }
        if dup {
            errorText = LocalizedStrings.errorDuplicateSensorName
            return
        }

        guard let mp = Double(megapixels), mp > 0.0 else {
            errorText = LocalizedStrings.errorMegapixelsInvalid
            return
        }

        guard let ps = Double(pixelSize), ps > 0.0 else {
            errorText = LocalizedStrings.errorPixelSizeInvalid
            return
        }

        // Map option to internal type
        let bType: String
        switch binning {
        case "Quad Bayer (2x2)": bType = "QUAD_BAYER"
        case "Nona (3x3)": bType = "NONA_BAYER"
        case "16-cell (4x4)": bType = "HEXADECA_BAYER"
        case "Unknown": bType = "UNKNOWN"
        default: bType = "NONE"
        }

        let newEntry = CustomSensorEntry(
            id: initial?.id ?? UUID().uuidString,
            name: nameTrimmed,
            megapixels: mp,
            pixelSizeUm: ps,
            binningType: bType
        )

        sensorRepository.upsertSensor(newEntry)
        onDismiss()
    }
}
