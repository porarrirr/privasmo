import Foundation

public func sanitizeDecimalInput(raw: String) -> String {
    if raw.isEmpty { return "" }
    var normalized = ""
    for char in raw {
        let ascii: Character
        switch char {
        case "０"..."９":
            let code = Int(char.unicodeScalars.first!.value) - Int(Character("０").unicodeScalars.first!.value) + Int(Character("0").unicodeScalars.first!.value)
            ascii = Character(UnicodeScalar(code)!)
        case "．", "。", "，", ",":
            ascii = "."
        default:
            ascii = char
        }
        if ascii.isNumber || ascii == "." {
            normalized.append(ascii)
        }
    }
    guard let firstDot = normalized.firstIndex(of: ".") else {
        return normalized
    }
    let beforeDot = normalized[..<firstDot]
    let afterDot = normalized[firstDot...].filter { $0 != "." }
    return String(beforeDot) + "." + String(afterDot)
}

public func sanitizeHexInput(raw: String) -> String {
    if raw.isEmpty { return "#" }
    let cleaned = raw.uppercased()
        .replacingOccurrences(of: "#", with: "")
        .filter { isHexDigit($0) }
        .prefix(6)
    return cleaned.isEmpty ? "#" : "#\(cleaned)"
}

public func parseHexColor(input: String) -> String? {
    let cleaned = input.uppercased().trimmingCharacters(in: CharacterSet(charactersIn: "#"))
    return (cleaned.count == 6 && cleaned.allSatisfy { isHexDigit($0) }) ? "#\(cleaned)" : nil
}

private func isHexDigit(_ char: Character) -> Bool {
    return char.isNumber || ("A"..."F").contains(char)
}
