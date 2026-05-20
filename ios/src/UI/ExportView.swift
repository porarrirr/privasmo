import SwiftUI

public struct ExportView: View {
    let results: ComparisonResults
    let metricType: ChartMetricType
    let selectedFocalLength: Double
    let settings: GraphSettings
    let exportedAt: Date
    
    public init(results: ComparisonResults, metricType: ChartMetricType, selectedFocalLength: Double, settings: GraphSettings, exportedAt: Date = Date()) {
        self.results = results
        self.metricType = metricType
        self.selectedFocalLength = selectedFocalLength
        self.settings = settings
        self.exportedAt = exportedAt
    }
    
    public var body: some View {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy/MM/dd HH:mm"
        let exportedAtLabel = formatter.string(from: exportedAt)
        
        let startFocal = results.focalLengths.first ?? 14.0
        let endFocal = results.focalLengths.last ?? 260.0
        
        return VStack(alignment: .leading, spacing: 16) {
            Text(LocalizedStrings.exportTitle)
                .font(.title2.bold())
                .foregroundColor(.primary)
                
            Text(String(format: LocalizedStrings.exportLabelCreatedAt, exportedAtLabel))
                .font(.caption)
                .foregroundColor(.secondary)
                
            Text(String(format: LocalizedStrings.exportLabelRange, String(format: "%.0f", startFocal), String(format: "%.0f", endFocal)))
                .font(.caption)
                .foregroundColor(.secondary)
                
            ComparisonChart(
                results: results,
                metricType: metricType,
                lineWidth: settings.lineWidth
            )
        }
        .padding(24)
        .background(Color(uiColor: .systemBackground))
    }
}
