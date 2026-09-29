package com.aistudio.couplebubble.qxztrw.data

import android.content.Context
import java.io.File

object LocalImageStorage {
    fun saveImage(context: Context, memoryId: String, imageBytes: ByteArray): String? {
        return try {
            val dir = File(context.filesDir, "memories")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "memory_$memoryId.jpg")
            file.writeBytes(imageBytes)
            "file://${file.absolutePath}"
        } catch (e: Exception) {
            null
        }
    }
}
