import SwiftUI

public struct PresetSaveSheet: View {
    @ObservedObject var viewModel: SensorComparisonViewModel
    @Environment(\.dismiss) var dismiss
    
    public init(viewModel: SensorComparisonViewModel) {
        self.viewModel = viewModel
    }
    
    public var body: some View {
        NavigationView {
            Form {
                Section(header: Text(LocalizedStrings.descriptionSelectedDeviceSaved)) {
                    TextField(LocalizedStrings.labelPresetName, text: Binding(
                        get: { viewModel.state.presetNameInput },
                        set: { viewModel.updatePresetNameInput($0) }
                    ))
                    
                    Picker(LocalizedStrings.labelPresetSaveTargetDeviceField, selection: Binding(
                        get: { viewModel.state.presetTargetDeviceId },
                        set: { viewModel.updatePresetTargetDevice($0) }
                    )) {
                        ForEach(viewModel.state.devices) { device in
                            Text(device.name).tag(Optional(device.id))
                        }
                    }
                }
                
                if let error = viewModel.state.presetErrorMessage {
                    Section {
                        Text(error)
                            .foregroundColor(.red)
                            .font(.caption)
                    }
                }
            }
            .navigationTitle(LocalizedStrings.labelPresetSaveTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(LocalizedStrings.actionCancel) {
                        viewModel.closePresetSheet()
                        dismiss()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(LocalizedStrings.actionSave) {
                        viewModel.savePreset()
                        if viewModel.state.presetSheet == .none {
                            dismiss()
                        }
                    }
                    .disabled(!viewModel.state.isPresetSaveEnabled)
                }
            }
        }
    }
}

public struct PresetLibrarySheet: View {
    @ObservedObject var viewModel: SensorComparisonViewModel
    @Environment(\.dismiss) var dismiss
    
    @State private var isShowingRenameAlert = false
    @State private var renameTargetId: String? = nil
    @State private var renameText = ""
    
    @State private var isShowingOverwriteAlert = false
    @State private var overwriteTargetPresetId: String? = nil
    
    public init(viewModel: SensorComparisonViewModel) {
        self.viewModel = viewModel
    }
    
    public var body: some View {
        NavigationView {
            VStack {
                if viewModel.state.presetListItems.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "folder.badge.minus")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        Text(LocalizedStrings.textNoPresetsRegistered)
                            .foregroundColor(.secondary)
                            .font(.body)
                    }
                    .padding()
                    .frame(maxHeight: .infinity)
                } else {
                    List {
                        Section(header: Text(LocalizedStrings.descriptionPresetAddOrOverwrite)) {
                            ForEach(viewModel.state.presetListItems) { item in
                                PresetItemRowView(
                                    item: item,
                                    showOverwrite: !viewModel.state.devices.isEmpty,
                                    onRename: { id, name in
                                        renameText = name
                                        renameTargetId = id
                                        isShowingRenameAlert = true
                                    },
                                    onAdd: { id in
                                        if viewModel.state.devices.count >= MAX_DEVICES {
                                            viewModel.state.presetErrorMessage = String(format: LocalizedStrings.errorMaxDevicesReached, MAX_DEVICES)
                                            return
                                        }
                                        viewModel.loadPreset(id)
                                        dismiss()
                                    },
                                    onOverwrite: { id in
                                        overwriteTargetPresetId = id
                                        isShowingOverwriteAlert = true
                                    },
                                    onDelete: { id in
                                        viewModel.deletePreset(id)
                                    }
                                )
                            }
                        }
                    }
                }
                
                if let error = viewModel.state.presetErrorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                        .padding()
                }
            }
            .navigationTitle(LocalizedStrings.titlePresetLibrary)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(LocalizedStrings.actionClose) {
                        viewModel.closePresetSheet()
                        dismiss()
                    }
                }
            }
            .alert(LocalizedStrings.dialogRenamePresetTitle, isPresented: $isShowingRenameAlert) {
                TextField(LocalizedStrings.labelName, text: $renameText)
                Button(LocalizedStrings.actionSave) {
                    if let pid = renameTargetId {
                        viewModel.renamePreset(pid, newName: renameText)
                    }
                }
                Button(LocalizedStrings.actionCancel, role: .cancel) {}
            }
            .alert(LocalizedStrings.dialogTitleOverwrite, isPresented: $isShowingOverwriteAlert) {
                Button(LocalizedStrings.dialogConfirmOverwrite, role: .destructive) {
                    if let pid = overwriteTargetPresetId {
                        viewModel.overwriteTargetDeviceFromPreset(presetId: pid)
                        if viewModel.state.presetSheet == .none {
                            dismiss()
                        }
                    }
                }
                Button(LocalizedStrings.actionCancel, role: .cancel) {}
            } message: {
                let targetId = viewModel.state.presetTargetDeviceId
                let targetName = targetId.flatMap { id in viewModel.state.devices.first(where: { $0.id == id })?.name } ?? LocalizedStrings.labelUntitledDevice
                Text(String(format: LocalizedStrings.dialogOverwriteTargetMessage, targetName))
            }
        }
    }
}

struct PresetItemRowView: View {
    let item: PresetListItem
    let showOverwrite: Bool
    let onRename: (String, String) -> Void
    let onAdd: (String) -> Void
    let onOverwrite: (String) -> Void
    let onDelete: (String) -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Circle()
                    .fill(Color(hex: item.colorHex) ?? .blue)
                    .frame(width: 12, height: 12)
                Text(item.name)
                    .font(.headline)
                    .foregroundColor(.primary)
            }
            
            HStack {
                Text(String(format: LocalizedStrings.labelDeviceNameWithValue, item.deviceName))
                Spacer()
                Text(String(format: LocalizedStrings.labelLensCountWithValue, item.lensCount))
            }
            .font(.caption)
            .foregroundColor(.secondary)
            
            HStack {
                let date = Date(timeIntervalSince1970: Double(item.updatedAtEpochMillis) / 1000)
                let formatter = DateFormatter()
                let _ = { formatter.dateFormat = "yyyy/MM/dd HH:mm" }()
                Text(String(format: LocalizedStrings.labelLastUpdated, formatter.string(from: date)))
                    .font(.caption2)
                    .foregroundColor(.secondary)
                
                Spacer()
                
                HStack(spacing: 16) {
                    Button(action: {
                        onRename(item.id, item.name)
                    }) {
                        Text(LocalizedStrings.actionRename)
                            .font(.caption.bold())
                            .foregroundColor(.accentColor)
                    }
                    .buttonStyle(PlainButtonStyle())
                    
                    Button(action: {
                        onAdd(item.id)
                    }) {
                        Text(LocalizedStrings.actionAdd)
                            .font(.caption.bold())
                            .foregroundColor(.green)
                    }
                    .buttonStyle(PlainButtonStyle())
                    
                    if showOverwrite {
                        Button(action: {
                            onOverwrite(item.id)
                        }) {
                            Text(LocalizedStrings.actionOverwrite)
                                .font(.caption.bold())
                                .foregroundColor(.orange)
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
            }
        }
        .padding(.vertical, 4)
        .swipeActions(edge: .trailing) {
            Button(role: .destructive) {
                onDelete(item.id)
            } label: {
                Label(LocalizedStrings.actionDelete, systemImage: "trash")
            }
        }
    }
}
