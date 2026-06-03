import SwiftUI

public struct SensorComparisonScreen: View {
    @AppStorage("app_language_override") var appLanguage: String = ""

    @StateObject private var viewModel = SensorComparisonViewModel()
    @StateObject private var settingsRepository = GraphSettingsRepository()

    @State private var selectedTab = 0
    @State private var selectedDeviceId: Int64? = nil
    @State private var isShowingSettings = false
    @State private var isShowingSavePresetSheet = false
    @State private var isShowingLoadPresetSheet = false

    public init() {}

    public var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Segmented picker for tabs
                Picker("Tabs", selection: $selectedTab) {
                    Text(LocalizedStrings.tabDeviceInput).tag(0)
                    Text(LocalizedStrings.tabComparisonGraph).tag(1)
                }
                .pickerStyle(SegmentedPickerStyle())
                .padding(.horizontal)
                .padding(.vertical, 8)

                // Tab Content
                if selectedTab == 0 {
                    deviceInputTab
                } else {
                    comparisonGraphTab
                }
            }
            .navigationTitle(LocalizedStrings.sensorComparisonTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    HStack(spacing: 12) {
                        Button(action: {
                            viewModel.openPresetSave()
                            isShowingSavePresetSheet = true
                        }) {
                            Image(systemName: "square.and.arrow.down")
                        }

                        Button(action: {
                            viewModel.openPresetLibrary()
                            isShowingLoadPresetSheet = true
                        }) {
                            Image(systemName: "folder")
                        }
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        isShowingSettings = true
                    }) {
                        Image(systemName: "gearshape")
                    }
                }
            }
            .sheet(isPresented: $isShowingSettings) {
                GraphSettingsScreen()
            }
            .sheet(isPresented: $isShowingSavePresetSheet) {
                PresetSaveSheet(viewModel: viewModel)
            }
            .sheet(isPresented: $isShowingLoadPresetSheet) {
                PresetLibrarySheet(viewModel: viewModel)
            }
            .overlay(alignment: .bottom) {
                if let msg = viewModel.toastMessage {
                    Text(msg)
                        .font(.footnote.bold())
                        .foregroundColor(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Color.black.opacity(0.8))
                        .cornerRadius(20)
                        .shadow(radius: 4)
                        .padding(.bottom, 40)
                        .onAppear {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                                withAnimation {
                                    viewModel.toastMessage = nil
                                }
                            }
                        }
                }
            }
        }
    }

    // TAB 1: Device Inputs
    private var deviceInputTab: some View {
        VStack {
            ScrollView {
                VStack(spacing: 16) {
                    deviceSelector

                    if let device = selectedInputDevice {
                        DeviceCardView(viewModel: viewModel, device: device)
                    }
                }
                .padding()
            }
        }
        .onAppear {
            syncSelectedDevice()
        }
        .onChange(of: viewModel.state.devices) { _ in
            syncSelectedDevice()
        }
    }

    private var selectedInputDevice: DeviceInputState? {
        if let selectedDeviceId,
           let selected = viewModel.state.devices.first(where: { $0.id == selectedDeviceId }) {
            return selected
        }
        return viewModel.state.devices.first
    }

    private var deviceSelector: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(LocalizedStrings.tabDeviceInput)
                    .font(.subheadline.bold())
                    .foregroundColor(.secondary)

                Spacer()

                Text("\(viewModel.state.devices.count)/\(MAX_DEVICES)")
                    .font(.caption.bold())
                    .foregroundColor(.secondary)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(viewModel.state.devices) { device in
                        Button(action: {
                            selectedDeviceId = device.id
                        }) {
                            DeviceSelectorChip(
                                device: device,
                                isSelected: selectedInputDevice?.id == device.id
                            )
                        }
                        .buttonStyle(PlainButtonStyle())
                    }

                    Button(action: {
                        viewModel.addDevice()
                        selectedDeviceId = viewModel.state.devices.last?.id
                    }) {
                        HStack(spacing: 6) {
                            Image(systemName: "plus")
                            Text(LocalizedStrings.actionAddDevice)
                        }
                        .font(.subheadline.bold())
                        .padding(.horizontal, 14)
                        .frame(height: 44)
                        .background(Color.accentColor.opacity(viewModel.state.canAddDevice ? 0.12 : 0.04))
                        .foregroundColor(viewModel.state.canAddDevice ? .accentColor : .secondary)
                        .cornerRadius(12)
                    }
                    .buttonStyle(PlainButtonStyle())
                    .disabled(!viewModel.state.canAddDevice)
                }
                .padding(.vertical, 2)
            }
        }
        .padding(12)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .cornerRadius(12)
    }

    private func syncSelectedDevice() {
        let devices = viewModel.state.devices
        guard !devices.isEmpty else {
            selectedDeviceId = nil
            return
        }

        if let selectedDeviceId,
           devices.contains(where: { $0.id == selectedDeviceId }) {
            return
        }

        selectedDeviceId = devices.first?.id
    }

    // TAB 2: Comparison Graph
    private var comparisonGraphTab: some View {
        VStack {
            if let results = viewModel.state.comparisonResults {
                ScrollView {
                    VStack(spacing: 20) {
                        ComparisonChart(
                            results: results,
                            metricType: .effectiveArea,
                            lineWidth: settingsRepository.settings.lineWidth
                        )
                        .padding(.horizontal)

                        ComparisonChart(
                            results: results,
                            metricType: .lightIntake,
                            lineWidth: settingsRepository.settings.lineWidth
                        )
                        .padding(.horizontal)

                        // Share & Export Actions
                        HStack(spacing: 16) {
                            Button(action: {
                                shareChartImage(metricType: .effectiveArea)
                            }) {
                                HStack {
                                    Image(systemName: "square.and.arrow.up")
                                    Text(LocalizedStrings.actionShareAreaChart)
                                }
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.accentColor)
                                .foregroundColor(.white)
                                .cornerRadius(10)
                            }

                            Button(action: {
                                shareChartImage(metricType: .lightIntake)
                            }) {
                                HStack {
                                    Image(systemName: "square.and.arrow.up")
                                    Text(LocalizedStrings.actionShareLightIntakeChart)
                                }
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.accentColor)
                                .foregroundColor(.white)
                                .cornerRadius(10)
                            }
                        }
                        .padding()
                    }
                }
            } else {
                VStack(spacing: 12) {
                    Image(systemName: "chart.xyaxis.line")
                        .font(.system(size: 64))
                        .foregroundColor(.secondary)
                    Text(LocalizedStrings.messageGenerateGraphFirst)
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }
                .frame(maxHeight: .infinity)
            }
        }
    }

    // Share image action helper
    @MainActor
    private func shareChartImage(metricType: ChartMetricType) {
        guard let results = viewModel.state.comparisonResults else { return }

        let exportView = ExportView(
            results: results,
            metricType: metricType,
            selectedFocalLength: viewModel.state.selectedFocalLength,
            settings: settingsRepository.settings
        )

        // Setup Aspect Ratio Frame in rendering
        let aspect = CGFloat(settingsRepository.settings.exportAspectRatio)
        let targetWidth: CGFloat = 800.0
        let targetHeight: CGFloat = targetWidth / aspect

        let renderer = ImageRenderer(content: exportView.frame(width: targetWidth, height: targetHeight))
        renderer.scale = 3.0 // High quality

        if let image = renderer.uiImage {
            let activityVC = UIActivityViewController(activityItems: [image], applicationActivities: nil)

            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let rootVC = windowScene.windows.first?.rootViewController {
                if let popover = activityVC.popoverPresentationController {
                    popover.sourceView = rootVC.view
                    popover.sourceRect = CGRect(x: rootVC.view.bounds.midX, y: rootVC.view.bounds.midY, width: 0, height: 0)
                    popover.permittedArrowDirections = []
                }
                rootVC.present(activityVC, animated: true)
            }
        }
    }

    private func dismissKeyboard() {
        UIApplication.shared.sendAction(
            #selector(UIResponder.resignFirstResponder),
            to: nil,
            from: nil,
            for: nil
        )
    }
}

private struct DeviceSelectorChip: View {
    let device: DeviceInputState
    let isSelected: Bool

    var body: some View {
        HStack(spacing: 8) {
            Circle()
                .fill(Color(hex: device.colorHex) ?? .blue)
                .frame(width: 12, height: 12)

            VStack(alignment: .leading, spacing: 2) {
                Text(device.name.isEmpty ? LocalizedStrings.labelName : device.name)
                    .font(.subheadline.bold())
                    .lineLimit(1)

                Text(String(format: LocalizedStrings.labelLensCountCompact, device.lenses.count))
                    .font(.caption2.bold())
                    .foregroundColor(isSelected ? .white.opacity(0.82) : .secondary)
            }
        }
        .padding(.horizontal, 12)
        .frame(height: 44)
        .background(isSelected ? Color.accentColor : Color(uiColor: .systemBackground))
        .foregroundColor(isSelected ? .white : .primary)
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(isSelected ? Color.accentColor : Color(uiColor: .separator).opacity(0.35), lineWidth: 1)
        )
    }
}
