package com.aistudio.couplebubble.qxztrw.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

object LocalImageStorage {

    fun saveImage(context: Context, memoryId: String, imageBytes: ByteArray): String? {
        return saveImage(context, memoryId, "a", imageBytes)
    }

    fun saveImage(context: Context, memoryId: String, slot: String, imageBytes: ByteArray): String? {
        return try {
            val dir = File(context.filesDir, "memories")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "memory_${memoryId}_${slot}.jpg")
            file.writeBytes(imageBytes)
            "file://${file.absolutePath}"
        } catch (e: Exception) {
            null
        }
    }

    fun saveProfilePhoto(context: Context, coupleId: String, isPartner1: Boolean, imageBytes: ByteArray): String? {
        return try {
            val dir = File(context.filesDir, "profiles")
            if (!dir.exists()) dir.mkdirs()
            val slot = if (isPartner1) "partner1" else "partner2"
            val file = File(dir, "profile_${coupleId}_${slot}.jpg")
            file.writeBytes(imageBytes)
            "file://${file.absolutePath}?t=${System.currentTimeMillis()}"
        } catch (e: Exception) {
            null
        }
    }

    suspend fun loadSampledBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 1200): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return@withContext null

            var inSampleSize = 1
            val largestDim = max(origWidth, origHeight)
            while ((largestDim / (inSampleSize * 2)) >= maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Client-side image compression according to Firebase Spark Plan constraints:
     * - Target max resolution: 1200px
     * - JPEG quality: 80%
     * - Output target: < 400 KB
     */
    fun compressImage(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1200,
        quality: Int = 80
    ): ByteArray? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var inSampleSize = 1
            val largestDim = max(origWidth, origHeight)
            while ((largestDim / (inSampleSize * 2)) >= maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // Precise scaling if still exceeds maxDimension
            val scaleFactor = max(sampledBitmap.width, sampledBitmap.height).toFloat() / maxDimension.toFloat()
            val finalBitmap = if (scaleFactor > 1.0f) {
                val targetW = (sampledBitmap.width / scaleFactor).toInt().coerceAtLeast(1)
                val targetH = (sampledBitmap.height / scaleFactor).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(sampledBitmap, targetW, targetH, true).also {
                    if (it != sampledBitmap) sampledBitmap.recycle()
                }
            } else {
                sampledBitmap
            }

            val stream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            finalBitmap.recycle()
            stream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Downloads an image from [imageUrl] via Coil and saves it to the device's
     * Pictures/CoupleBubble directory via MediaStore.
     *
     * - Android 10+ (API 29+): Uses Scoped Storage (no write permission required).
     * - Android 8.0 - 9.0 (API 26-28): Requires WRITE_EXTERNAL_STORAGE permission.
     */
    suspend fun saveImageToGallery(context: Context, imageUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    throw SecurityException("WRITE_EXTERNAL_STORAGE permission not granted")
                }
            }

            val normalizedUrl = if (imageUrl.startsWith("/")) "file://$imageUrl" else imageUrl

            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(normalizedUrl)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)
            val bitmap = if (result is SuccessResult) {
                val drawable = result.drawable
                (drawable as? BitmapDrawable)?.bitmap ?: drawable.toBitmap()
            } else if (normalizedUrl.startsWith("file://")) {
                val filePath = normalizedUrl.removePrefix("file://")
                BitmapFactory.decodeFile(filePath) ?: throw IllegalStateException("Failed to decode local image file")
            } else {
                throw IllegalStateException("Failed to load image via Coil")
            }

            val filename = "CoupleBubble_${System.currentTimeMillis()}.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CoupleBubble")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException("Failed to insert MediaStore entry")

                try {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
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
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                        throw IllegalStateException("Failed to compress bitmap")
                    }
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    @Suppress("DEPRECATION")
                    put(MediaStore.Images.Media.DATA, targetFile.absolutePath)
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            }
            Unit
        }
    }
}
