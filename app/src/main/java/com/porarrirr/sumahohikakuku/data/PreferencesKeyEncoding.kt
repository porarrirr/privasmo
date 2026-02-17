package com.porarrirr.sumahohikakuku.data

private val HEX_CHARS = charArrayOf(
    '0', '1', '2', '3', '4', '5', '6', '7',
    '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
)

/**
 * Encode an arbitrary string into a Preferences key-safe component.
 *
 * Preferences DataStore key names are restricted to `[A-Za-z0-9_]`.
 * This function encodes the UTF-8 bytes as lowercase hex so it never throws when used in a key name.
 */
internal fun encodePreferencesKeyComponent(raw: String): String {
    val bytes = raw.toByteArray(Charsets.UTF_8)
    val out = CharArray(bytes.size * 2)
    var outIndex = 0
    for (byte in bytes) {
        val value = byte.toInt() and 0xFF
        out[outIndex++] = HEX_CHARS[value ushr 4]
        out[outIndex++] = HEX_CHARS[value and 0x0F]
    }
    return String(out)
}

