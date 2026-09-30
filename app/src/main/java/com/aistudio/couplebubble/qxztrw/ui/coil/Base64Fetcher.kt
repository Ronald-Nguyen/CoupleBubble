package com.aistudio.couplebubble.qxztrw.ui.coil

import android.util.Base64
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer

/**
 * Custom Coil Fetcher that decodes Base64 data URIs and raw Base64 image strings.
 * Ensures cross-device and offline image display resilience without relying on
 * accessible device-local filesystem sandbox paths.
 */
class Base64Fetcher(
    private val data: String,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val base64Content = if (data.contains(",")) {
            data.substringAfter(",")
        } else {
            data
        }
        val cleanBase64 = base64Content.trim().replace("\n", "").replace("\r", "")
        val bytes = try {
            Base64.decode(cleanBase64, Base64.DEFAULT)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid Base64 image string", e)
        }

        val mimeType = when {
            data.startsWith("data:image/png", ignoreCase = true) || cleanBase64.startsWith("iVBORw0KGgo") -> "image/png"
            data.startsWith("data:image/webp", ignoreCase = true) || cleanBase64.startsWith("UklGR") -> "image/webp"
            data.startsWith("data:image/gif", ignoreCase = true) || cleanBase64.startsWith("R0lGOD") -> "image/gif"
            else -> "image/jpeg"
        }

        val buffer = Buffer().write(bytes)
        SourceResult(
            source = ImageSource(source = buffer, context = options.context),
            mimeType = mimeType,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            return if (isBase64Data(data)) {
                Base64Fetcher(data, options)
            } else {
                null
            }
        }

        private fun isBase64Data(data: String): Boolean {
            val trimmed = data.trim()
            if (trimmed.startsWith("data:image/", ignoreCase = true)) return true
            if (trimmed.startsWith("data:", ignoreCase = true) && trimmed.contains("base64,")) return true
            // Support raw Base64 image signatures (JPEG: /9j/, PNG: iVBORw0KGgo, WebP: UklGR)
            if (trimmed.length > 50 && !trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.startsWith("file://") && !trimmed.startsWith("content://")) {
                if (trimmed.startsWith("/9j/") || trimmed.startsWith("iVBORw0KGgo") || trimmed.startsWith("UklGR")) {
                    return true
                }
            }
            return false
        }
    }
}
