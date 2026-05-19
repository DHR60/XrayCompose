package com.clearpath.xray_compose.utils

import kotlin.io.encoding.Base64

object Base64Util {
    private val VALID_CHARS = BooleanArray(256).apply {
        for (c in 'A'..'Z') this[c.code] = true
        for (c in 'a'..'z') this[c.code] = true
        for (c in '0'..'9') this[c.code] = true
        this['+'.code] = true
        this['/'.code] = true
        this['-'.code] = true
        this['_'.code] = true
    }

    @JvmStatic
    fun isBase64Char(ch: Char): Boolean {
        val code = ch.code
        return code in 0..255 && VALID_CHARS[code]
    }

    /**
     * A variant-friendly check to determine if a string is a valid Base64 or Base64URL payload.
     * Supports unpadded (trimmed trailing equals) variants automatically.
     *
     * @param text The string to check.
     * @param requirePure If true, returns false if the string contains any noise (whitespaces, line breaks, or misplaced equals).
     * @return True if the string can be successfully decoded into a byte array, false otherwise.
     */
    fun isBase64String(text: String?, requirePure: Boolean = true): Boolean {
        if (text.isNullOrEmpty()) return false
        var effectiveLength = 0
        var hasSeenPadding = false

        for (i in text.indices) {
            val ch = text[i]

            when {
                isBase64Char(ch) -> {
                    // In strict pure mode, valid characters cannot appear after a padding '=' character.
                    if (requirePure && hasSeenPadding) return false
                    effectiveLength++
                }

                ch == '=' -> {
                    hasSeenPadding = true
                    // In strict pure mode, '=' can only appear at the very end (the last or second-to-last position).
                    if (requirePure && i < text.length - 2) {
                        if (i == text.length - 2 && text[text.length - 1] != '=') return false
                    }
                }

                else -> {
                    // Garbage/noise characters (e.g., spaces, newlines, special symbols)
                    if (requirePure) return false
                }
            }
        }

        // Mathematical Proof: The length of a valid Base64 payload can never be 4n + 1.
        // 1 Base64 character yields only 6 bits, which is insufficient to decode even a single 8-bit byte.
        // Lengths where remainder is 0, 2, or 3 can all be safely decoded.
        return effectiveLength % 4 != 1
    }

    fun decodeBase64(text: String?): String {
        return tryDecodeBase64(text) ?: text?.trimEnd('=')?.let { tryDecodeBase64(it) }.orEmpty()
    }

    fun tryDecodeBase64(text: String?): String? {
        if (text.isNullOrEmpty()) return null

        try {
            val decodedBytes = Base64.decode(text)
            return String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            LogUtil.e("Failed to decode standard base64", e)
        }
        try {
            val decodedBytes = Base64.UrlSafe.decode(text)
            return String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            LogUtil.e("Failed to decode URL-safe base64", e)
        }
        return null
    }

    fun encodeBase64(text: String, removePadding: Boolean = false): String {
        return try {
            val encodedString = Base64.encode(text.toByteArray(Charsets.UTF_8))
            if (removePadding) encodedString.trimEnd('=') else encodedString
        } catch (e: Exception) {
            LogUtil.e("Failed to encode standard base64", e)
            ""
        }
    }

    fun encodeBase64UrlSafe(text: String, removePadding: Boolean = false): String {
        return try {
            val encodedString = Base64.UrlSafe.encode(text.toByteArray(Charsets.UTF_8))
            if (removePadding) encodedString.trimEnd('=') else encodedString
        } catch (e: Exception) {
            LogUtil.e("Failed to encode URL-safe base64", e)
            ""
        }
    }

    /**
     * Decodes a Base64 string with maximum tolerance.
     * Automatically filters out all non-Base64/URL-Safe characters, normalizes variants,
     * handles missing padding, and attempts to recover the original payload.
     *
     * @param text The Base64 string to decode.
     * @return A [ByteArray] containing the decoded data, or an empty array if recovery fails.
     */
    fun decodeBase64WithMaxTolerance(text: String?): ByteArray {
        if (text.isNullOrEmpty()) return ByteArray(0)
        val sb = StringBuilder(text.length)

        // Step 1: Filter and Normalize
        for (i in text.indices) {
            val ch = text[i]
            when {
                // Retain standard characters
                (ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9') -> sb.append(ch)
                // Normalize URL-Safe variants to Standard Base64
                (ch == '+' || ch == '-') -> sb.append('+')
                (ch == '/' || ch == '_') -> sb.append('/')
                // Explicit padding characters are ignored here; padding will be re-calculated later.
                ch == '=' -> { /* Skip explicit padding */
                }
            }
        }

        // Step 2: Drop dangling characters that cannot form a 6-bit block
        val len = sb.length
        if (len == 0) return ByteArray(0)
        if (len % 4 == 1) {
            sb.setLength(len - 1)
        }

        // Step 3: Re-add standard Base64 padding '=' based on the actual length
        when (sb.length % 4) {
            2 -> sb.append("==")
            3 -> sb.append("=")
        }

        // Step 4: Decode using the built-in standard decoder
        return try {
            Base64.decode(sb.toString())
        } catch (e: IllegalArgumentException) {
            LogUtil.e("Failed to decode base64 with max tolerance", e)
            // Fallback for extreme/malformed edge cases
            ByteArray(0)
        }
    }
}