import SwiftUI

public struct ResultsSectionView: View {
    let results: ComparisonResults
    let selectedFocalLength: Double
    let availableSensors: [SensorSpec]
    
    @State private var isExpanded = false
    
    public init(results: ComparisonResults, selectedFocalLength: Double, availableSensors: [SensorSpec]) {
        self.results = results
        self.selectedFocalLength = selectedFocalLength
        self.availableSensors = availableSensors
    }
    
    public var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text(String(format: LocalizedStrings.exportSectionSpecsAt, String(format: "%.0f", selectedFocalLength)))
                    .font(.headline)
                Spacer()
                Button(action: {
                    withAnimation(.spring()) {
                        isExpanded.toggle()
                    }
                }) {
                    Text(isExpanded ? LocalizedStrings.actionHideDetails : LocalizedStrings.actionShowDetails)
                        .font(.caption.bold())
                        .foregroundColor(.accentColor)
                }
            }
            .padding(.horizontal)
            
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(alignment: .top, spacing: 16) {
                    ForEach(results.devices) { device in
                        if let metric = device.metricsByFocalLength.first(where: { abs($0.focalLength35mm - selectedFocalLength) < 0.01 }) {
                            deviceSpecCard(device: device, metric: metric)
                        }
                    }
                }
                .padding(.horizontal)
                .padding(.bottom, 6)
            }
        }
    }
    
    private func deviceSpecCard(device: ProcessedDevice, metric: FocalLengthMetrics) -> some View {
        let color = Color(hex: device.colorHex) ?? .blue
        
        return VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack {
                Circle()
                    .fill(color)
                    .frame(width: 12, height: 12)
                Text(device.name)
                    .font(.subheadline.bold())
                    .lineLimit(1)
            }
            .padding(.bottom, 4)
            
            // Core metrics
            VStack(alignment: .leading, spacing: 8) {
                specRow(label: LocalizedStrings.metricTotalLightIntake, value: String(format: "%.2f", metric.totalLightIntake), highlight: true)
                specRow(label: LocalizedStrings.metricEffectiveArea, value: String(format: "%.2f mm²", metric.effectiveAreaSqMm))
                specRow(label: LocalizedStrings.labelEstimatedFNumber, value: String(format: "f/%.2f", metric.effectiveFNumber))
                specRow(label: LocalizedStrings.labelOpticalZoomRatio, value: String(format: "%.2fx", metric.opticalZoomRatio))
                specRow(label: LocalizedStrings.labelDigitalCropRatio, value: String(format: "%.2fx", metric.digitalCropRatio))
            }
            
            // Collapsible extra details
            if isExpanded {
                VStack(spacing: 8) {
                    Divider()
                        .padding(.vertical, 4)
                    
                    specRow(label: LocalizedStrings.labelVariableOpticalRange, value: opticalRangeValue(for: metric))
                    specRow(label: LocalizedStrings.metricActualFocalLength, value: String(format: "%.1f mm", metric.opticalActualFocalLengthMm))
                    specRow(label: LocalizedStrings.metricEffectiveAperture, value: String(format: "%.2f mm", metric.apertureDiameterMm))
                    specRow(label: LocalizedStrings.metricApertureArea, value: String(format: "%.2f mm²", metric.apertureAreaSqMm))
                }
                .transition(.opacity)
            }
        }
        .padding()
        .frame(width: 200)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.04), radius: 6, x: 0, y: 3)
    }
    
    private func specRow(label: String, value: String, highlight: Bool = false) -> some View {
        HStack {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .font(.caption.bold())
                .foregroundColor(highlight ? .accentColor : .primary)
        }
    }

    private func opticalRangeValue(for metric: FocalLengthMetrics) -> String {
        if metric.baseLens.isVariableOptical {
            return String(
                format: "%.0f-%.0f mm",
                metric.baseLens.nativeFocalLength35mm,
                metric.baseLens.opticalEndFocalLength35mm
            )
        }
        return String(format: "%.0f mm", metric.baseLens.nativeFocalLength35mm)
    }
}
