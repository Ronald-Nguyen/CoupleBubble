package com.aistudio.couplebubble.qxztrw.data

import android.util.Base64

/**
 * Normalizes the image references stored with memories and profiles: remote URLs, local `file://` /
 * `content://` URIs, data URIs and raw Base64 payloads (which older builds sometimes saved as `file:///9j...`).
 */
object ImageUrls {

    /** Leading characters of Base64-encoded JPEG, PNG and WebP files, mapped to their MIME type. */
    val BASE64_SIGNATURES: Map<String, String> = linkedMapOf(
        "/9j" to "image/jpeg",
        "iVBORw0KGgo" to "image/png",
        "UklGR" to "image/webp",
    )

    private const val FILE_SCHEME = "file://"

    /** A raw Base64 payload wrongly prefixed with `file://` (e.g. `file:///9j...`). */
    fun isFileWrappedBase64(url: String): Boolean =
        BASE64_SIGNATURES.keys.any { url.startsWith(FILE_SCHEME + it) }

    /** Turns `file://<base64>` into a data URI; anything else is returned unchanged. */
    fun healFileBase64(url: String): String {
        val signature = BASE64_SIGNATURES.entries.firstOrNull { url.startsWith(FILE_SCHEME + it.key) } ?: return url
        return "data:${signature.value};base64,${url.removePrefix(FILE_SCHEME)}"
    }

    private fun rawBase64DataUri(trimmed: String): String? =
        BASE64_SIGNATURES.entries.firstOrNull { trimmed.startsWith(it.key) }?.let { "data:${it.value};base64,$trimmed" }

    /** Returns a URL Coil and Firestore can handle, or `null` for blank or unrecognizable input. */
    fun sanitize(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return when {
            isFileWrappedBase64(trimmed) -> healFileBase64(trimmed)
            trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith(FILE_SCHEME, ignoreCase = true) ||
                trimmed.startsWith("content://", ignoreCase = true) ||
                trimmed.startsWith("data:image/", ignoreCase = true) -> trimmed
            trimmed.startsWith("data:", ignoreCase = true) && trimmed.contains("base64,") -> trimmed
            rawBase64DataUri(trimmed) != null -> rawBase64DataUri(trimmed)
            trimmed.startsWith("/") -> "$FILE_SCHEME$trimmed"
            trimmed.length > 50 && !trimmed.contains(" ") && !trimmed.contains("://") -> "data:image/jpeg;base64,$trimmed"
            else -> null
        }
    }

    /** Whether [url] points into this device's sandbox and must never be synced to the partner. */
    fun isLocalUri(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        if (isFileWrappedBase64(trimmed)) return false
        return trimmed.startsWith(FILE_SCHEME, ignoreCase = true) ||
            trimmed.startsWith("content://", ignoreCase = true) ||
            (trimmed.startsWith("/") && !trimmed.startsWith("/9j"))
    }

    /** Whether [data] is an unprefixed Base64 image payload. */
    fun isRawBase64(data: String): Boolean {
        val trimmed = data.trim()
        return BASE64_SIGNATURES.keys.any { trimmed.startsWith(it) } && trimmed.length > 50
    }

    fun jpegDataUri(bytes: ByteArray): String =
        "data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
}
