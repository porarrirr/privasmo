package com.porarrirr.sumahohikakuku.domain.input

import java.util.Locale

fun sanitizeDecimalInput(raw: String): String {
    if (raw.isEmpty()) return ""
    val normalized = buildString(raw.length) {
        for (char in raw) {
            val ascii = when (char) {
                in '０'..'９' -> (char.code - '０'.code + '0'.code).toChar()
                '．', '。', '，', ',' -> '.'
                else -> char
            }
            if (ascii.isDigit() || ascii == '.') append(ascii)
        }
    }
    val firstDot = normalized.indexOf('.')
    if (firstDot == -1) return normalized
    return normalized.substring(0, firstDot + 1) + normalized.substring(firstDot + 1).filter { it != '.' }
}

fun sanitizeHexInput(raw: String): String {
    if (raw.isEmpty()) return "#"
    val cleaned = raw.uppercase(Locale.US)
        .replace("#", "")
        .filter(::isHexDigit)
        .take(6)
    return if (cleaned.isEmpty()) "#" else "#$cleaned"
}

fun parseHexColor(input: String): String? {
    val cleaned = input.uppercase(Locale.US).removePrefix("#")
    return if (cleaned.length == 6 && cleaned.all(::isHexDigit)) "#$cleaned" else null
}

private fun isHexDigit(char: Char): Boolean {
    return char in '0'..'9' || char in 'A'..'F'
}
