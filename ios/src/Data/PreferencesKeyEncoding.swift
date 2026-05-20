import Foundation

internal func encodePreferencesKeyComponent(_ raw: String) -> String {
    let data = Data(raw.utf8)
    return data.map { String(format: "%02x", $0) }.joined()
}
