package com.aistudio.couplebubble.qxztrw.ui.coil

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.map.Mapper
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer

/**
 * Model representing an image payload encoded in Base64 (Data URI or raw string).
 * Wrapping this into a distinct type ensures Coil's built-in StringMapper does not
 * convert Base64 strings to android.net.Uri before reaching Base64Fetcher.
 */
data class Base64Image(val payload: String)

/**
 * Custom Coil Mapper running ahead of StringMapper to intercept Base64 strings
 * and map them to [Base64Image].
 */
class Base64Mapper : Mapper<String, Base64Image> {
    override fun map(data: String, options: Options): Base64Image? {
        return if (isBase64Data(data)) {
            Base64Image(data)
        } else {
            null
        }
    }

    companion object {
        fun isBase64Data(data: String): Boolean {
            var startIndex = 0
            while (startIndex < data.length && data[startIndex].isWhitespace()) {
                startIndex++
            }
            if (startIndex >= data.length) return false

            val remainingLength = data.length - startIndex
            if (data.startsWith("data:image/", startIndex, ignoreCase = true)) return true
            if (data.startsWith("data:", startIndex, ignoreCase = true) && data.indexOf("base64,", startIndex) != -1) return true
            if (data.startsWith("file:///9j", startIndex) ||
                data.startsWith("file://iVBORw0KGgo", startIndex) ||
                data.startsWith("file://UklGR", startIndex)
            ) return true

            // Support raw Base64 image signatures (JPEG: /9j, PNG: iVBORw0KGgo, WebP: UklGR)
            if (remainingLength > 50 &&
                !data.startsWith("http://", startIndex) &&
                !data.startsWith("https://", startIndex) &&
                !data.startsWith("content://", startIndex) &&
                !data.startsWith("file://", startIndex)
            ) {
                if (data.startsWith("/9j", startIndex) ||
                    data.startsWith("iVBORw0KGgo", startIndex) ||
                    data.startsWith("UklGR", startIndex)
                ) {
                    return true
                }
            }
            return false
        }
    }
}

/**
 * Cache keyer for [Base64Image] ensuring decoded bitmaps are stored in and retrieved
 * from Coil's memory cache across recompositions and list scrolling.
 */
class Base64Keyer : Keyer<Base64Image> {
    override fun key(data: Base64Image, options: Options): String {
        return "b64:${data.payload.length}:${data.payload.hashCode()}"
    }
}

/**
 * Custom Coil Fetcher that decodes Base64 data URIs and raw Base64 image strings.
 * Ensures cross-device and offline image display resilience without relying on
 * accessible device-local filesystem sandbox paths.
 */
class Base64Fetcher(
    private val image: Base64Image,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val rawPayload = image.payload
        val base64Content = when {
            rawPayload.contains(",") -> rawPayload.substringAfter(",")
            rawPayload.startsWith("file://") -> rawPayload.removePrefix("file://")
            else -> rawPayload
        }
        val cleanBase64 = base64Content.trim().replace("\n", "").replace("\r", "")
        val bytes = try {
            Base64.decode(cleanBase64, Base64.DEFAULT)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid Base64 image string", e)
        }

        // 1. Prefer direct BitmapDrawable decoding: 0ms overhead, avoids stream exhaustion in BitmapFactoryDecoder
        val bitmap = try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }

        if (bitmap != null) {
            DrawableResult(
                drawable = BitmapDrawable(options.context.resources, bitmap),
                isSampled = false,
                dataSource = DataSource.MEMORY
            )
        } else {
            val mimeType = when {
                rawPayload.startsWith("data:image/png", ignoreCase = true) || cleanBase64.startsWith("iVBORw0KGgo") -> "image/png"
                rawPayload.startsWith("data:image/webp", ignoreCase = true) || cleanBase64.startsWith("UklGR") -> "image/webp"
                rawPayload.startsWith("data:image/gif", ignoreCase = true) || cleanBase64.startsWith("R0lGOD") -> "image/gif"
                else -> "image/jpeg"
            }

            val buffer = Buffer().write(bytes)
            SourceResult(
                source = ImageSource(source = buffer, context = options.context),
                mimeType = mimeType,
                dataSource = DataSource.MEMORY
            )
        }
    }

    class Factory : Fetcher.Factory<Base64Image> {
        override fun create(data: Base64Image, options: Options, imageLoader: ImageLoader): Fetcher {
            return Base64Fetcher(data, options)
        }
    }
}
