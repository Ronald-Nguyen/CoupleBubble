package com.aistudio.couplebubble.qxztrw.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Device-local image work the ViewModel needs, without handing it a [Context]. */
interface LocalImageStore {
    /** Saves a local-first copy of a memory photo and returns its `file://` URI. */
    fun saveMemoryImage(memoryId: String, slot: String, bytes: ByteArray): String?

    /** Reads, rotates and compresses a picked photo (see [LocalImageStorage.compressImage]). */
    suspend fun compress(uri: Uri): ByteArray?

    suspend fun saveToGallery(imageUrl: String): Result<Unit>
}

class AndroidLocalImageStore(private val appContext: Context) : LocalImageStore {
    override fun saveMemoryImage(memoryId: String, slot: String, bytes: ByteArray): String? =
        LocalImageStorage.saveImage(appContext, memoryId, slot, bytes)

    override suspend fun compress(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        LocalImageStorage.compressImage(appContext, uri)
    }

    override suspend fun saveToGallery(imageUrl: String): Result<Unit> =
        LocalImageStorage.saveImageToGallery(appContext, imageUrl)
}
