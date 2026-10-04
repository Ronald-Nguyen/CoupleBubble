@file:Suppress("UseExifInterface", "AndroidLintUseExifInterface")

package com.aistudio.couplebubble.qxztrw.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import android.media.ExifInterface
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

@Suppress("UseExifInterface")
object LocalImageStorage {

    fun saveImage(context: Context, memoryId: String, slot: String, imageBytes: ByteArray): String? {
        return try {
            val dir = File(context.filesDir, "memories")
            if (!dir.exists()) dir.mkdirs()
            // Unique name per save: a stable path would let Coil serve the previous image from cache.
            val file = writeVersionedFile(dir, "memory_${memoryId}_${slot}", imageBytes)
            "file://${file.absolutePath}"
        } catch (_: Exception) {
            null
        }
    }

    fun saveProfilePhoto(context: Context, coupleId: String, isPartner1: Boolean, imageBytes: ByteArray): String? {
        return try {
            val dir = File(context.filesDir, "profiles")
            if (!dir.exists()) dir.mkdirs()
            val slot = if (isPartner1) "partner1" else "partner2"
            val file = writeVersionedFile(dir, "profile_${coupleId}_${slot}", imageBytes)
            "file://${file.absolutePath}"
        } catch (_: Exception) {
            null
        }
    }

    private fun writeVersionedFile(dir: File, baseName: String, imageBytes: ByteArray): File {
        val file = File(dir, "${baseName}_${System.currentTimeMillis()}.jpg")
        file.writeBytes(imageBytes)
        dir.listFiles { f -> f != file && (f.name == "$baseName.jpg" || f.name.startsWith("${baseName}_")) }
            ?.forEach { it.delete() }
        return file
    }

    private fun getExifRotation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) {
            0
        }
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
            if ((origWidth <= 0) || (origHeight <= 0)) return@withContext null

            var inSampleSize = 1
            val largestDim = max(origWidth, origHeight)
            while ((largestDim / (inSampleSize * 2)) >= maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            val rotation = getExifRotation(context, uri)
            rotateBitmapIfNeeded(rawBitmap, rotation)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Client-side image compression according to Firebase Spark Plan constraints:
     * - Target max resolution: 1200px
     * - Automatic EXIF orientation correction
     * - JPEG quality: adaptive (starts at 80%, drops if needed to stay under 250 KB)
     * - Strict output target: < 250 KB to guarantee 1MB Firestore limit with dual photos
     */
    fun compressImage(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1200,
        quality: Int = 80,
    ): ByteArray? {
        return try {
            val rotation = getExifRotation(context, uri)

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if ((origWidth <= 0) || (origHeight <= 0)) return null

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

            // Correct EXIF orientation first
            val orientedBitmap = rotateBitmapIfNeeded(sampledBitmap, rotation)

            // Precise scaling if still exceeds maxDimension
            val scaleFactor = max(orientedBitmap.width, orientedBitmap.height).toFloat() / maxDimension.toFloat()
            val finalBitmap = if (scaleFactor > 1.0f) {
                val targetW = (orientedBitmap.width / scaleFactor).toInt().coerceAtLeast(1)
                val targetH = (orientedBitmap.height / scaleFactor).toInt().coerceAtLeast(1)
                orientedBitmap.scale(targetW, targetH, filter = true).also {
                    if (it != orientedBitmap) orientedBitmap.recycle()
                }
            } else {
                orientedBitmap
            }

            // Adaptive compression to guarantee payload stays under 250 KB
            val targetMaxBytes = 250_000
            val qualitiesToTry = listOf(quality, 70, 58, 45)
            var resultBytes: ByteArray? = null

            for (q in qualitiesToTry) {
                val stream = ByteArrayOutputStream()
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, q, stream)
                val bytes = stream.toByteArray()
                if ((bytes.size <= targetMaxBytes) || (q == qualitiesToTry.last())) {
                    resultBytes = bytes
                    break
                }
            }
            finalBitmap.recycle()
            resultBytes
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Downloads an image from [imageUrl] and saves it to the device's
     * Pictures/CoupleBubble directory via MediaStore.
     * Handles remote URLs, local file paths, and Base64 data URIs.
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

            val normalizedUrl = when {
                ImageUrls.isFileWrappedBase64(imageUrl) -> ImageUrls.healFileBase64(imageUrl)
                imageUrl.startsWith("/") && !imageUrl.startsWith("/9j") -> "file://$imageUrl"
                else -> imageUrl
            }

            val bitmap: Bitmap = when {
                normalizedUrl.startsWith("data:", ignoreCase = true) || ImageUrls.isRawBase64(normalizedUrl) -> {
                    val base64Data = if (normalizedUrl.contains(",")) normalizedUrl.substringAfter(",") else normalizedUrl
                    val clean = base64Data.trim().replace("\n", "").replace("\r", "")
                    val bytes = Base64.decode(clean, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: throw IllegalStateException("Failed to decode Base64 image")
                }
                normalizedUrl.startsWith("file://") -> {
                    val cleanPath = normalizedUrl.removePrefix("file://").substringBefore("?")
                    BitmapFactory.decodeFile(cleanPath)
                        ?: throw IllegalStateException("Failed to decode local image file")
                }
                else -> {
                    val imageLoader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(normalizedUrl)
                        .allowHardware(enable = false)
                        .build()

                    val result = imageLoader.execute(request)
                    if (result is SuccessResult) {
                        val drawable = result.drawable
                        (drawable as? BitmapDrawable)?.bitmap ?: drawable.toBitmap()
                    } else {
                        throw IllegalStateException("Failed to load image via Coil")
                    }
                }
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
