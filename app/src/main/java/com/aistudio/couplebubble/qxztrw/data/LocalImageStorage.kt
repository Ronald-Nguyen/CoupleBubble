package com.aistudio.couplebubble.qxztrw.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
}
