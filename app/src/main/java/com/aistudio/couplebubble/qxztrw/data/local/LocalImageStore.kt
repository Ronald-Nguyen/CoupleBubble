package com.aistudio.couplebubble.qxztrw.data.local

import android.graphics.Bitmap
import android.net.Uri

/** Device-local image work the ViewModels and repositories need, without handing them a Context. */
interface LocalImageStore {
    /** Saves a local-first copy of a memory photo and returns its `file://` URI. */
    fun saveMemoryImage(memoryId: String, slot: String, bytes: ByteArray): String?

    /** Saves a local-first copy of a profile photo, named after its owner's UID, and returns its `file://` URI. */
    fun saveProfilePhoto(coupleId: String, ownerUid: String?, bytes: ByteArray): String?

    /** Reads, rotates and compresses a picked photo to a JPEG under 250 KB. */
    suspend fun compress(uri: Uri): ByteArray?

    /** Decodes a picked photo upright and downsampled to roughly [maxDimension] pixels, e.g. for cropping. */
    suspend fun loadSampledBitmap(uri: Uri, maxDimension: Int = 1200): Bitmap?

    suspend fun saveToGallery(imageUrl: String): Result<Unit>
}
