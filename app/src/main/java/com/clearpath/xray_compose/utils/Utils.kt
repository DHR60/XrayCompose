package com.clearpath.xray_compose.utils

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.io.encoding.Base64

object Utils {
    private val IPV4_REGEX =
        Regex("^([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])$")
    private val IPV6_REGEX =
        Regex("^([0-9A-Fa-f]{1,4})?(:[0-9A-Fa-f]{1,4})*::([0-9A-Fa-f]{1,4})?(:[0-9A-Fa-f]{1,4})*|([0-9A-Fa-f]{1,4})(:[0-9A-Fa-f]{1,4}){7}$")

    fun getClipboard(context: Context): String {
        return try {
            val cmb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cmb.primaryClip?.getItemAt(0)?.text.toString()
        } catch (e: Exception) {
            LogUtil.e("Failed to get clipboard content", e)
            ""
        }
    }

    fun setClipboard(context: Context, text: String) {
        try {
            val cmb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cmb.setPrimaryClip(android.content.ClipData.newPlainText("text", text))
        } catch (e: Exception) {
            LogUtil.e("Failed to set clipboard content", e)
        }
    }

    fun decode(text: String?): String {
        return tryDecodeBase64(text) ?: text?.trimEnd('=')?.let { tryDecodeBase64(it) }.orEmpty()
    }

    fun tryDecodeBase64(text: String?): String? {
        if (text.isNullOrEmpty()) return null

        try {
            val decodedBytes = Base64.Default.decode(text)
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

    fun encode(text: String, removePadding: Boolean = false): String {
        return try {
            val encodedString = Base64.encode(text.toByteArray(Charsets.UTF_8))
            if (removePadding) encodedString.trimEnd('=') else encodedString
        } catch (e: Exception) {
            LogUtil.e("Failed to encode standard base64", e)
            ""
        }
    }

    fun isIpAddress(value: String?): Boolean {
        if (value.isNullOrEmpty()) return false
        return isPureIpAddress(value) || isIpCIDR(value)
    }

    fun isIpCIDR(value: String?): Boolean {
        if (value.isNullOrEmpty()) return false

        val parts = value.split("/")
        if (parts.size != 2) return false

        val ipPart = parts[0]
        val prefixPart = parts[1]

        val maxPrefix = when {
            IPV4_REGEX.matches(ipPart) -> 32
            IPV6_REGEX.matches(ipPart) -> 128
            else -> return false
        }

        val prefixLength = prefixPart.toIntOrNull() ?: return false
        return prefixLength in 0..maxPrefix
    }

    fun isPureIpAddress(value: String): Boolean {
        return isIpv4Address(value) || isIpv6Address(value)
    }

    fun isIpv4Address(value: String): Boolean {
        return IPV4_REGEX.matches(value)
    }

    fun isIpv6Address(value: String): Boolean {
        var addr = value
        if (addr.startsWith("[") && addr.endsWith("]")) {
            addr = addr.drop(1).dropLast(1)
        }
        return IPV6_REGEX.matches(addr)
    }

    fun isDomain(value: String?): Boolean {
        if (value.isNullOrEmpty()) return false
        if (isIpAddress(value)) return false
        return true
    }

    fun isCoreDNSAddress(s: String): Boolean {
        return s.startsWith("https") ||
                s.startsWith("tcp") ||
                s.startsWith("quic") ||
                s == "localhost" ||
                isPureIpAddress(s)
    }

    fun urlDecode(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        return try {
            URLDecoder.decode(text, Charsets.UTF_8.name())
        } catch (e: Exception) {
            LogUtil.e("Failed to URL decode", e)
            text
        }
    }

    fun urlEncode(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        return try {
            URLEncoder.encode(text, Charsets.UTF_8.name())
        } catch (e: Exception) {
            LogUtil.e("Failed to URL encode", e)
            text
        }
    }

    fun decodeURIComponent(url: String): String {
        return try {
            // Decode strictly according to RFC 3986 / encodeURIComponent semantics.
            // '+' is a literal plus and MUST NOT be interpreted as space.
            // Inputs using '+' for spaces are non-conforming and rejected deliberately
            // to avoid cross-language interoperability issues.
            URLDecoder.decode(url.replace("+", "%2B"), Charsets.UTF_8.toString())
        } catch (e: Exception) {
            LogUtil.e("Failed to decode encodeURIComponent", e)
            url
        }
    }

    fun encodeURIComponent(text: String): String {
        return try {
            // Encode strictly according to RFC 3986 / encodeURIComponent semantics.
            // Unreserved characters MUST NOT be percent-encoded.
            // Space is encoded as '%20' and MUST NOT be encoded as '+'.
            URLEncoder.encode(text, Charsets.UTF_8.toString())
                .replace("+", "%20")
        } catch (e: Exception) {
            LogUtil.e("Failed to encode encodeURIComponent", e)
            text
        }
    }

    fun getDeviceIdForXUDPBaseKey(): String {
        return try {
            val androidId = android.provider.Settings.Secure.ANDROID_ID.toByteArray(Charsets.UTF_8)
            val paddedBytes = androidId.copyOf(32)
            // Base64.encodeToString(androidId.copyOf(32), Base64.NO_PADDING.or(Base64.URL_SAFE))
            Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(paddedBytes)
        } catch (e: Exception) {
            LogUtil.e("Failed to generate device ID", e)
            ""
        }
    }

    fun getIpv6Address(address: String?): String {
        if (address.isNullOrEmpty()) return ""

        return if (isIpv6Address(address) && !address.contains('[') && !address.contains(']')) {
            "[$address]"
        } else {
            address
        }
    }

    fun findFreePort(ports: List<Int>): Int {
        for (port in ports) {
            try {
                return java.net.ServerSocket(port).use { it.localPort }
            } catch (ex: java.io.IOException) {
                continue  // try next port
            }
        }

        // if the program gets here, no port in the range was found
        throw java.io.IOException("no free port found")
    }

    fun findRandomFreePort(): Int {
        return java.net.ServerSocket(0).use { it.localPort }
    }

    fun userAssetPath(context: Context?): String {
        if (context == null) return ""

        return try {
            context.getExternalFilesDir("assets")?.absolutePath
                ?: context.getDir("assets", 0).absolutePath
        } catch (e: Exception) {
            LogUtil.e("Failed to get user asset path", e)
            ""
        }
    }

    fun receiverFlags(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.RECEIVER_EXPORTED
    } else {
        ContextCompat.RECEIVER_NOT_EXPORTED
    }
}