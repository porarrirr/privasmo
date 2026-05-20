import SwiftUI

public struct SensorComparisonScreen: View {
    @StateObject private var viewModel = SensorComparisonViewModel()
    @StateObject private var settingsRepository = GraphSettingsRepository()
    
    @State private var selectedTab = 0
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
                    Text(LocalizedStrings.tabSensorDetails).tag(2)
                }
                .pickerStyle(SegmentedPickerStyle())
                .padding(.horizontal)
                .padding(.vertical, 8)
                
                // Tab Content
                if selectedTab == 0 {
                    deviceInputTab
                } else if selectedTab == 1 {
                    comparisonGraphTab
                } else {
                    sensorDetailsTab
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
                    ForEach(viewModel.state.devices) { device in
                        DeviceCardView(viewModel: viewModel, device: device)
                    }
                    
                    Button(action: {
                        viewModel.addDevice()
                    }) {
                        HStack {
                            Image(systemName: "plus.circle.fill")
                            Text(LocalizedStrings.actionAddDevice)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.accentColor.opacity(0.1))
                        .foregroundColor(.accentColor)
                        .cornerRadius(12)
                    }
                    .disabled(viewModel.state.devices.count >= MAX_DEVICES)
                    .padding(.top, 8)
                }
                .padding()
            }
            
            // Bottom floating action to update graph
            Button(action: {
                viewModel.generateComparison()
                selectedTab = 1 // Switch to graph automatically
            }) {
                Text(LocalizedStrings.buttonGenerateGraph)
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(viewModel.state.isGenerateEnabled ? Color.accentColor : Color.gray)
                    .cornerRadius(12)
                    .shadow(radius: 4)
            }
            .disabled(!viewModel.state.isGenerateEnabled)
            .padding()
        }
    }
    
    // TAB 2: Comparison Graph
    private var comparisonGraphTab: some View {
        VStack {
            if let results = viewModel.state.comparisonResults {
                ScrollView {
                    VStack(spacing: 20) {
                        // Slider for focal length control
                        VStack(alignment: .leading, spacing: 8) {
                            Text(String(format: LocalizedStrings.labelSelectedFocalLength, String(format: "%.0f", viewModel.state.selectedFocalLength)))
                                .font(.subheadline.bold())
                            
                            Slider(
                                value: Binding(
                                    get: { viewModel.state.selectedFocalLength },
                                    set: { viewModel.updateFocalLength(focalLength: $0) }
                                ),
                                in: (viewModel.state.focalLengths.first ?? 14.0)...(viewModel.state.focalLengths.last ?? 260.0)
                            )
                        }
                        .padding()
                        .background(Color(uiColor: .secondarySystemGroupedBackground))
                        .cornerRadius(12)
                        .padding(.horizontal)
                        
                        ComparisonChart(
                            results: results,
                            metricType: .effectiveArea,
                            selectedFocalLength: viewModel.state.selectedFocalLength,
                            lineWidth: settingsRepository.settings.lineWidth
                        ) { focal in
                            viewModel.updateFocalLength(focalLength: focal)
                        }
                        .padding(.horizontal)
                        
                        ComparisonChart(
                            results: results,
                            metricType: .lightIntake,
                            selectedFocalLength: viewModel.state.selectedFocalLength,
                            lineWidth: settingsRepository.settings.lineWidth
                        ) { focal in
                            viewModel.updateFocalLength(focalLength: focal)
                        }
                        .padding(.horizontal)
                        
                        // Share & Export Actions
                        HStack(spacing: 16) {
                            Button(action: {
                                shareChartImage(metricType: .effectiveArea)
                            }) {
                                HStack {
                                    Image(systemName: "square.and.arrow.up")
                                    Text("面積グラフ共有")
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
                                    Text("集光力グラフ共有")
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
    
    // TAB 3: Sensor Details
    private var sensorDetailsTab: some View {
        VStack {
            if let results = viewModel.state.comparisonResults {
                VStack(spacing: 12) {
                    // Slider to change focal length
                    VStack(alignment: .leading, spacing: 8) {
                        Text(String(format: LocalizedStrings.labelSelectedFocalLength, String(format: "%.0f", viewModel.state.selectedFocalLength)))
                            .font(.subheadline.bold())
                        
                        Slider(
                            value: Binding(
                                get: { viewModel.state.selectedFocalLength },
                                set: { viewModel.updateFocalLength(focalLength: $0) }
                            ),
                            in: (viewModel.state.focalLengths.first ?? 14.0)...(viewModel.state.focalLengths.last ?? 260.0)
                        )
                    }
                    .padding()
                    .background(Color(uiColor: .secondarySystemGroupedBackground))
                    .cornerRadius(12)
                    .padding(.horizontal)
                    .padding(.top)
                    
                    ResultsSectionView(
                        results: results,
                        selectedFocalLength: viewModel.state.selectedFocalLength,
                        availableSensors: viewModel.state.availableSensors
                    )
                    
                    Spacer()
                }
            } else {
                VStack(spacing: 12) {
                    Image(systemName: "tablecells")
                        .font(.system(size: 64))
                        .foregroundColor(.secondary)
                    Text(LocalizedStrings.messageGenerateSensorDetailsFirst)
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
}
