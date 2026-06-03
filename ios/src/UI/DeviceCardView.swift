import SwiftUI

public struct DeviceCardView: View {
    @ObservedObject var viewModel: SensorComparisonViewModel
    let device: DeviceInputState
    
    @State private var activeLensPicker: LensPickerPresentation? = nil
    
    public init(viewModel: SensorComparisonViewModel, device: DeviceInputState) {
        self.viewModel = viewModel
        self.device = device
    }
    
    public var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Header: Name & Delete button
            HStack {
                TextField(LocalizedStrings.labelName, text: Binding(
                    get: { device.name },
                    set: { viewModel.updateDeviceName(deviceId: device.id, name: $0) }
                ))
                .font(.title3.bold())
                .textFieldStyle(RoundedBorderTextFieldStyle())
                
                Spacer()
                
                Button(action: {
                    viewModel.removeDevice(deviceId: device.id)
                }) {
                    Image(systemName: "trash")
                        .foregroundColor(.red)
                }
                .buttonStyle(PlainButtonStyle())
            }
            
            // Color picker & presets indicator
            HStack(spacing: 12) {
                // Circular color selection buttons
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(viewModel.state.availableDeviceColors, id: \.self) { colorHex in
                            Circle()
                                .fill(Color(hex: colorHex) ?? .blue)
                                .frame(width: 28, height: 28)
                                .overlay(
                                    Circle()
                                        .stroke(Color.primary, lineWidth: device.colorHex == colorHex ? 2 : 0)
                                )
                                .onTapGesture {
                                    viewModel.updateDeviceColor(deviceId: device.id, colorHex: colorHex)
                                }
                        }
                        
                        // Custom ColorPicker wrapper
                        ColorPicker("", selection: Binding(
                            get: { Color(hex: device.colorHex) ?? .blue },
                            set: { color in
                                if let hex = color.toHex() {
                                    viewModel.updateDeviceColor(deviceId: device.id, colorHex: hex)
                                }
                            }
                        ))
                        .labelsHidden()
                        .frame(width: 28, height: 28)
                    }
                }
                
                Spacer()
                
                // Show if it is associated with a preset
                if let presetId = viewModel.state.activePresetAssignments[device.id],
                   let presetName = viewModel.state.presets.first(where: { $0.id == presetId })?.name {
                    HStack(spacing: 4) {
                        Image(systemName: "bookmark.fill")
                            .font(.caption)
                        Text(presetName)
                            .font(.caption2.bold())
                    }
                    .foregroundColor(.accentColor)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.accentColor.opacity(0.1))
                    .cornerRadius(8)
                }
            }
            
            Divider()
            
            // Lenses header & add lens button
            HStack {
                Text(LocalizedStrings.labelLens)
                    .font(.headline)
                
                Spacer()
                
                Button(action: {
                    viewModel.addLens(deviceId: device.id)
                }) {
                    HStack(spacing: 4) {
                        Image(systemName: "plus")
                        Text(String(format: LocalizedStrings.buttonAddLensWithMax, MAX_LENSES_PER_DEVICE))
                    }
                    .font(.caption.bold())
                }
                .disabled(device.lenses.count >= MAX_LENSES_PER_DEVICE)
            }
            
            // Lenses List
            VStack(spacing: 12) {
                ForEach(device.lenses) { lens in
                    LensRowView(
                        lens: lens,
                        availableSensors: viewModel.state.availableSensors,
                        onFocalLengthChanged: { val in
                            viewModel.updateLensFocalLength(deviceId: device.id, lensId: lens.id, value: val)
                        },
                        onFNumberChanged: { val in
                            viewModel.updateLensFNumber(deviceId: device.id, lensId: lens.id, value: val)
                        },
                        onOpticalEndFocalLengthChanged: { val in
                            viewModel.updateLensOpticalEndFocalLength(deviceId: device.id, lensId: lens.id, value: val)
                        },
                        onEndFNumberChanged: { val in
                            viewModel.updateLensEndFNumber(deviceId: device.id, lensId: lens.id, value: val)
                        },
                        onSensorTap: {
                            activeLensPicker = LensPickerPresentation(lensId: lens.id)
                        },
                        onManualDescriptorChanged: { val in
                            viewModel.updateLensManualDescriptor(deviceId: device.id, lensId: lens.id, descriptor: val)
                        },
                        onDelete: {
                            viewModel.removeLens(deviceId: device.id, lensId: lens.id)
                        }
                    )
                }
            }
        }
        .padding()
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.04), radius: 6, x: 0, y: 3)
        .sheet(item: $activeLensPicker) { picker in
            SensorPickerSheetView(viewModel: viewModel) { selectedValue in
                viewModel.updateLensSensorSelection(deviceId: device.id, lensId: picker.lensId, newValue: selectedValue)
                activeLensPicker = nil
            }
        }
    }
}

private struct LensPickerPresentation: Identifiable {
    let lensId: Int64
    var id: Int64 { lensId }
}

struct LensRowView: View {
    let lens: LensInputState
    let availableSensors: [SensorSpec]
    let onFocalLengthChanged: (String) -> Void
    let onFNumberChanged: (String) -> Void
    let onOpticalEndFocalLengthChanged: (String) -> Void
    let onEndFNumberChanged: (String) -> Void
    let onSensorTap: () -> Void
    let onManualDescriptorChanged: (String) -> Void
    let onDelete: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 12) {
                // Focal Length
                VStack(alignment: .leading, spacing: 4) {
                    Text(LocalizedStrings.labelFocalLength)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    TextField("24", text: Binding(
                        get: { lens.nativeFocalLength },
                        set: { onFocalLengthChanged($0) }
                    ))
                    .keyboardType(.decimalPad)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .frame(width: 80)
                }
                
                // F-Number
                VStack(alignment: .leading, spacing: 4) {
                    Text(LocalizedStrings.labelFNumber)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    TextField("1.8", text: Binding(
                        get: { lens.fNumber },
                        set: { onFNumberChanged($0) }
                    ))
                    .keyboardType(.decimalPad)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .frame(width: 60)
                }
                
                // Sensor Picker trigger button
                VStack(alignment: .leading, spacing: 4) {
                    Text(LocalizedStrings.labelSensor)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    
                    Button(action: onSensorTap) {
                        HStack {
                            let displayName = availableSensors.first(where: { $0.value == lens.selectedSensorValue })?.name ?? lens.selectedSensorValue
                            Text(displayName)
                                .font(.body)
                                .lineLimit(1)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                        .padding(.horizontal, 8)
                        .frame(height: 34)
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(6)
                    }
                    .buttonStyle(PlainButtonStyle())
                }
                
                // Delete button
                VStack {
                    Spacer()
                    Button(action: onDelete) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.secondary)
                    }
                    .buttonStyle(PlainButtonStyle())
                    .padding(.bottom, 6)
                }
            }

            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(LocalizedStrings.labelOpticalEndFocalLength)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    TextField("100", text: Binding(
                        get: { lens.opticalEndFocalLength },
                        set: { onOpticalEndFocalLengthChanged($0) }
                    ))
                    .keyboardType(.decimalPad)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .frame(width: 110)
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(LocalizedStrings.labelEndFNumber)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    TextField("2.96", text: Binding(
                        get: { lens.endFNumber },
                        set: { onEndFNumberChanged($0) }
                    ))
                    .keyboardType(.decimalPad)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .frame(width: 80)
                }

                Spacer()
            }
            
            // If manual input sensor, show manual field
            if lens.usesManualSensor {
                VStack(alignment: .leading, spacing: 4) {
                    Text(LocalizedStrings.labelManualInputExample)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    
                    TextField("1/1.33", text: Binding(
                        get: { lens.manualSensorDescriptor },
                        set: { onManualDescriptorChanged($0) }
                    ))
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .padding(8)
        .background(Color(.systemGroupedBackground).opacity(0.5))
        .cornerRadius(8)
    }
}
