import Foundation
import Combine

public struct GraphSettings: Codable, Equatable {
    public let lineWidth: Float
    public let exportAspectWidth: Int
    public let exportAspectHeight: Int
    
    public var glowLineWidth: Float {
        return lineWidth * GraphSettings.defaultGlowMultiplier
    }
    public var exportAspectRatio: Float {
        return Float(exportAspectWidth) / Float(exportAspectHeight)
    }
    
    public static let minLineWidth: Float = 1.0
    public static let maxLineWidth: Float = 8.0
    public static let defaultLineWidth: Float = 4.0
    public static let defaultGlowMultiplier: Float = 2.5
    public static let minExportAspectComponent: Int = 1
    public static let maxExportAspectComponent: Int = 100
    public static let defaultExportAspectWidth: Int = 4
    public static let defaultExportAspectHeight: Int = 3
    
    public init(lineWidth: Float = defaultLineWidth, exportAspectWidth: Int = defaultExportAspectWidth, exportAspectHeight: Int = defaultExportAspectHeight) {
        self.lineWidth = lineWidth
        self.exportAspectWidth = exportAspectWidth
        self.exportAspectHeight = exportAspectHeight
    }
}

public class GraphSettingsRepository: ObservableObject {
    @Published public var settings: GraphSettings = GraphSettings()
    
    private let lineWidthKey = "chart_line_width"
    private let exportAspectWidthKey = "export_aspect_width"
    private let exportAspectHeightKey = "export_aspect_height"
    
    public init() {
        loadSettings()
    }
    
    private func loadSettings() {
        let width = UserDefaults.standard.object(forKey: lineWidthKey) as? Float ?? GraphSettings.defaultLineWidth
        let clampedWidth = width.clamped(to: GraphSettings.minLineWidth...GraphSettings.maxLineWidth)
        
        let aspectWidth = UserDefaults.standard.object(forKey: exportAspectWidthKey) as? Int ?? GraphSettings.defaultExportAspectWidth
        let clampedAspectWidth = aspectWidth.clamped(to: GraphSettings.minExportAspectComponent...GraphSettings.maxExportAspectComponent)
        
        let aspectHeight = UserDefaults.standard.object(forKey: exportAspectHeightKey) as? Int ?? GraphSettings.defaultExportAspectHeight
        let clampedAspectHeight = aspectHeight.clamped(to: GraphSettings.minExportAspectComponent...GraphSettings.maxExportAspectComponent)
        
        self.settings = GraphSettings(
            lineWidth: clampedWidth,
            exportAspectWidth: clampedAspectWidth,
            exportAspectHeight: clampedAspectHeight
        )
    }
    
    public func setLineWidth(_ value: Float) {
        let clamped = value.clamped(to: GraphSettings.minLineWidth...GraphSettings.maxLineWidth)
        UserDefaults.standard.set(clamped, forKey: lineWidthKey)
        loadSettings()
    }
    
    public func setExportAspectRatio(width: Int, height: Int) {
        let clampedWidth = width.clamped(to: GraphSettings.minExportAspectComponent...GraphSettings.maxExportAspectComponent)
        let clampedHeight = height.clamped(to: GraphSettings.minExportAspectComponent...GraphSettings.maxExportAspectComponent)
        UserDefaults.standard.set(clampedWidth, forKey: exportAspectWidthKey)
        UserDefaults.standard.set(clampedHeight, forKey: exportAspectHeightKey)
        loadSettings()
    }
    
    public func reset() {
        UserDefaults.standard.removeObject(forKey: lineWidthKey)
        UserDefaults.standard.removeObject(forKey: exportAspectWidthKey)
        UserDefaults.standard.removeObject(forKey: exportAspectHeightKey)
        loadSettings()
    }
}

extension Comparable {
    func clamped(to limits: ClosedRange<Self>) -> Self {
        return min(max(self, limits.lowerBound), limits.upperBound)
    }
}
