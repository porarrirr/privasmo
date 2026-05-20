import SwiftUI
import Charts

public enum ChartMetricType {
    case effectiveArea
    case lightIntake
    
    public var title: String {
        switch self {
        case .effectiveArea: return LocalizedStrings.chartTitleEffectiveArea
        case .lightIntake: return LocalizedStrings.chartTitleLightIntake
        }
    }
    
    public var yAxisLabel: String {
        switch self {
        case .effectiveArea: return LocalizedStrings.chartYLabelEffectiveArea
        case .lightIntake: return LocalizedStrings.chartYLabelLightIntake
        }
    }
    
    public var unit: String {
        switch self {
        case .effectiveArea: return " mm²"
        case .lightIntake: return ""
        }
    }
}

public struct ComparisonChart: View {
    let results: ComparisonResults
    let metricType: ChartMetricType
    let selectedFocalLength: Double
    let lineWidth: Float
    let onFocalLengthChanged: (Double) -> Void
    
    @State private var dragFocalLength: Double? = nil
    
    public var body: some View {
        let displayFocal = dragFocalLength ?? selectedFocalLength
        return VStack(alignment: .leading, spacing: 12) {
            Text(metricType.title)
                .font(.headline)
                .foregroundColor(.primary)
            
            ChartContainerView(
                results: results,
                metricType: metricType,
                lineWidth: lineWidth,
                displayFocal: displayFocal,
                selectedFocalLength: selectedFocalLength,
                dragFocalLength: $dragFocalLength,
                onFocalLengthChanged: onFocalLengthChanged
            )
            
            ChartLegendView(devices: results.devices)
        }
        .padding()
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.04), radius: 6, x: 0, y: 3)
    }
}

extension Color {
    public init?(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        
        if hexSanitized.hasPrefix("#") {
            hexSanitized.remove(at: hexSanitized.startIndex)
        }
        
        if hexSanitized.count != 6 {
            return nil
        }
        
        var rgb: UInt64 = 0
        Scanner(string: hexSanitized).scanHexInt64(&rgb)
        
        let r = Double((rgb & 0xFF0000) >> 16) / 255.0
        let g = Double((rgb & 0x00FF00) >> 8) / 255.0
        let b = Double(rgb & 0x0000FF) / 255.0
        
        self.init(red: r, green: g, blue: b)
    }
    
    public func toHex() -> String? {
        guard let components = UIColor(self).cgColor.components, components.count >= 3 else {
            return nil
        }
        let r = Float(components[0])
        let g = Float(components[1])
        let b = Float(components[2])
        return String(format: "#%02lX%02lX%02lX", lroundf(r * 255), lroundf(g * 255), lroundf(b * 255))
    }
}

struct DeviceChartContent: ChartContent {
    let device: ProcessedDevice
    let metricType: ChartMetricType
    let lineWidth: Float
    
    var body: some ChartContent {
        let color = Color(hex: device.colorHex) ?? .blue
        let lenses = device.lenses
        ForEach(device.metricsByFocalLength, id: \.focalLength35mm) { metric in
            let yValue = (metricType == .effectiveArea) ? metric.effectiveAreaSqMm : metric.totalLightIntake
            let isNative = lenses.contains(where: { abs($0.nativeFocalLength35mm - metric.focalLength35mm) < 0.01 })
            
            LineMark(
                x: .value(LocalizedStrings.labelFocalLength, metric.focalLength35mm),
                y: .value(metricType.yAxisLabel, yValue),
                series: .value(LocalizedStrings.labelDeviceDefaultName, device.id)
            )
            .foregroundStyle(color)
            .lineStyle(StrokeStyle(lineWidth: CGFloat(lineWidth), lineCap: .round, lineJoin: .round))
            
            if isNative {
                PointMark(
                    x: .value(LocalizedStrings.labelFocalLength, metric.focalLength35mm),
                    y: .value(metricType.yAxisLabel, yValue)
                )
                .foregroundStyle(color)
                .symbolSize(CGFloat(lineWidth) * 12)
            }
        }
    }
}

struct RuleMarkAnnotationView: View {
    let focal: Double
    
    var body: some View {
        Text(String(format: "%.0fmm", focal))
            .font(.caption2.bold())
            .foregroundColor(.white)
            .padding(.horizontal, 6)
            .padding(.vertical, 3)
            .background(Color.accentColor)
            .cornerRadius(4)
            .shadow(radius: 2)
    }
}

struct ComparisonChartContent: ChartContent {
    let results: ComparisonResults
    let metricType: ChartMetricType
    let lineWidth: Float
    let displayFocal: Double
    
    var body: some ChartContent {
        ForEach(results.devices) { device in
            DeviceChartContent(device: device, metricType: metricType, lineWidth: lineWidth)
        }
        
        RuleMark(
            x: .value("Focal Length", displayFocal)
        )
        .foregroundStyle(Color.accentColor.opacity(0.6))
        .lineStyle(StrokeStyle(lineWidth: 1.5, dash: [4, 4]))
        .annotation(position: .top, alignment: .center) {
            RuleMarkAnnotationView(focal: displayFocal)
        }
    }
}

struct ChartLegendView: View {
    let devices: [ProcessedDevice]
    
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 16) {
                ForEach(devices) { device in
                    let color = Color(hex: device.colorHex) ?? .blue
                    HStack(spacing: 6) {
                        Circle()
                            .fill(color)
                            .frame(width: 10, height: 10)
                        Text(device.name)
                            .font(.caption.bold())
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding(.horizontal, 4)
        }
    }
}

struct ChartContainerView: View {
    let results: ComparisonResults
    let metricType: ChartMetricType
    let lineWidth: Float
    let displayFocal: Double
    let selectedFocalLength: Double
    @Binding var dragFocalLength: Double?
    let onFocalLengthChanged: (Double) -> Void
    
    private func handleDragChanged(value: DragGesture.Value, geometry: GeometryProxy, proxy: ChartProxy) {
        let x = value.location.x
        if let focalLength = proxy.value(atX: x, as: Double.self) {
            let minFocal = results.focalLengths.first ?? 14.0
            let maxFocal = results.focalLengths.last ?? 260.0
            let clamped = max(minFocal, min(focalLength, maxFocal))
            dragFocalLength = clamped
            onFocalLengthChanged(clamped)
        }
    }
    
    private func handleDragEnded() {
        dragFocalLength = nil
    }

    private var deviceColorDomain: [String] {
        results.devices.map(\.id)
    }

    private var deviceColorRange: [Color] {
        results.devices.map { device in
            Color(hex: device.colorHex) ?? .blue
        }
    }
    
    var body: some View {
        Chart {
            ComparisonChartContent(
                results: results,
                metricType: metricType,
                lineWidth: lineWidth,
                displayFocal: displayFocal
            )
        }
        .chartForegroundStyleScale(domain: deviceColorDomain, range: deviceColorRange)
        .chartXScale(domain: (results.focalLengths.first ?? 14.0)...(results.focalLengths.last ?? 260.0))
        .frame(height: 220)
        .padding(.top, 10)
        .chartOverlay { proxy in
            GeometryReader { geometry in
                Color.clear
                    .contentShape(Rectangle())
                    .gesture(
                        DragGesture(minimumDistance: 0)
                            .onChanged { value in
                                handleDragChanged(value: value, geometry: geometry, proxy: proxy)
                            }
                            .onEnded { _ in
                                handleDragEnded()
                            }
                    )
            }
        }
    }
}
