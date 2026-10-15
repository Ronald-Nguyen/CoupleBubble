@file:Suppress("UseExifInterface", "AndroidLintUseExifInterface")

package com.aistudio.couplebubble.qxztrw.data.local

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aistudio.couplebubble.qxztrw.core.di.AppDispatchers
import com.aistudio.couplebubble.qxztrw.core.rethrowIfCancellation
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.withContext

class AndroidLocalImageStore(
    private val appContext: Context,
    private val dispatchers: AppDispatchers = AppDispatchers()
) : LocalImageStore {

    private companion object {
        const val MAX_DIMENSION = 1200
        const val JPEG_QUALITY = 80
        const val GALLERY_QUALITY = 95

        // Two photos per memory must fit into one 1 MB Firestore document
        const val TARGET_MAX_BYTES = 250_000
        val FALLBACK_QUALITIES = listOf(70, 58, 45)
    }

    override suspend fun saveMemoryImage(memoryId: String, slot: String, bytes: ByteArray): String? =
        saveVersioned("memories", "memory_${memoryId}_${slot}", bytes)

    /** Named after the owner's UID, not the slot, so a role swap never makes one partner replace the other's file. */
    override suspend fun saveProfilePhoto(coupleId: String, ownerUid: String?, bytes: ByteArray): String? =
        saveVersioned("profiles", "profile_${coupleId}_${ownerUid ?: "local"}", bytes)

    private suspend fun saveVersioned(folder: String, baseName: String, bytes: ByteArray): String? = withContext(dispatchers.io) { try {
        val dir = File(appContext.filesDir, folder)
        if (!dir.exists()) dir.mkdirs()
        // Unique name per save: a stable path would let Coil serve the previous image from cache.
        val file = File(dir, "${baseName}_${System.currentTimeMillis()}.jpg")
        file.writeBytes(bytes)
        dir.listFiles { f -> f != file && (f.name == "$baseName.jpg" || f.name.startsWith("${baseName}_")) }
            ?.forEach { it.delete() }
        "file://${file.absolutePath}"
    } catch (_: Exception) {
        null
    } }

    override suspend fun loadSampledBitmap(uri: Uri, maxDimension: Int): Bitmap? = withContext(dispatchers.io) {
        try {
            decodeUpright(uri, maxDimension)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            null
        } catch (_: OutOfMemoryError) {
            // Low-memory devices: show no preview rather than killing the app
            null
        }
    }

    /**
     * Client-side image compression according to Firebase Spark Plan constraints: max 1200 px, EXIF orientation
     * corrected, JPEG quality starting at 80 % and dropping until the result is under 250 KB.
     */
    override suspend fun compress(uri: Uri): ByteArray? = withContext(dispatchers.io) {
        try {
            val oriented = decodeUpright(uri, MAX_DIMENSION) ?: return@withContext null

            // Precise scaling if sampling by powers of two left it larger than the limit
            val scaleFactor = max(oriented.width, oriented.height).toFloat() / MAX_DIMENSION
            val finalBitmap = if (scaleFactor > 1.0f) {
                val targetW = (oriented.width / scaleFactor).toInt().coerceAtLeast(1)
                val targetH = (oriented.height / scaleFactor).toInt().coerceAtLeast(1)
                oriented.scale(targetW, targetH, filter = true).also {
                    if (it != oriented) oriented.recycle()
                }
            } else {
                oriented
            }

            val qualities = listOf(JPEG_QUALITY) + FALLBACK_QUALITIES
            val result = qualities.firstNotNullOf { quality ->
                val bytes = ByteArrayOutputStream().also {
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, it)
                }.toByteArray()
                bytes.takeIf { it.size <= TARGET_MAX_BYTES || quality == qualities.last() }
            }
            finalBitmap.recycle()
            result
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    /** Decodes [uri] downsampled by powers of two to about [maxDimension] and rotates it upright per EXIF. */
    private fun decodeUpright(uri: Uri, maxDimension: Int): Bitmap? {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var inSampleSize = 1
        val largestDim = max(bounds.outWidth, bounds.outHeight)
        while ((largestDim / (inSampleSize * 2)) >= maxDimension) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sampled = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: return null
        return rotateBitmapIfNeeded(sampled, exifRotation(uri))
    }

    private fun exifRotation(uri: Uri): Int = try {
        appContext.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    } catch (_: Exception) {
        0
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        return try {
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                if (it != bitmap) bitmap.recycle()
            }
        } catch (_: Exception) {
            bitmap
        } catch (_: OutOfMemoryError) {
            bitmap
        }
    }

    /**
     * Saves the image behind [imageUrl] (remote URL, local file or Base64 data URI) to `Pictures/CoupleBubble`.
     * Android 10+ uses Scoped Storage without a permission; Android 8–9 needs WRITE_EXTERNAL_STORAGE.
     */
    override suspend fun saveToGallery(imageUrl: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                throw SecurityException("WRITE_EXTERNAL_STORAGE permission not granted")
            }
            writeToGallery(loadBitmap(imageUrl))
        }.onFailure { it.rethrowIfCancellation() }
    }

    private suspend fun loadBitmap(imageUrl: String): Bitmap {
        val normalizedUrl = when {
            ImageUrls.isFileWrappedBase64(imageUrl) -> ImageUrls.healFileBase64(imageUrl)
            imageUrl.startsWith("/") && !imageUrl.startsWith("/9j") -> "file://$imageUrl"
            else -> imageUrl
        }

        return when {
            normalizedUrl.startsWith("data:", ignoreCase = true) || ImageUrls.isRawBase64(normalizedUrl) -> {
                val base64Data = if (normalizedUrl.contains(",")) normalizedUrl.substringAfter(",") else normalizedUrl
                val bytes = Base64.decode(base64Data.trim().replace("\n", "").replace("\r", ""), Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw IllegalStateException("Failed to decode Base64 image")
            }
            normalizedUrl.startsWith("file://") -> {
                val cleanPath = normalizedUrl.removePrefix("file://").substringBefore("?")
                BitmapFactory.decodeFile(cleanPath)
                    ?: throw IllegalStateException("Failed to decode local image file")
            }
            else -> {
                val request = ImageRequest.Builder(appContext)
                    .data(normalizedUrl)
                    .allowHardware(enable = false)
                    .build()
                val result = appContext.imageLoader.execute(request) as? SuccessResult
                    ?: throw IllegalStateException("Failed to load image via Coil")
                (result.drawable as? BitmapDrawable)?.bitmap ?: result.drawable.toBitmap()
            }
        }
    }

    private fun writeToGallery(bitmap: Bitmap) {
        val filename = "CoupleBubble_${System.currentTimeMillis()}.jpg"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CoupleBubble")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = appContext.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("Failed to insert MediaStore entry")

            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, GALLERY_QUALITY, outputStream)) {
                        throw IllegalStateException("Failed to compress bitmap")
                    }
                } ?: throw IllegalStateException("Failed to open output stream")

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val targetDir = File(picturesDir, "CoupleBubble").apply {
                if (!exists()) mkdirs()
            }
            val targetFile = File(targetDir, filename)
            targetFile.outputStream().use { outputStream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, GALLERY_QUALITY, outputStream)) {
                    throw IllegalStateException("Failed to compress bitmap")
                }
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                @Suppress("DEPRECATION")
                put(MediaStore.Images.Media.DATA, targetFile.absolutePath)
            }
            appContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        }
    }
}
