package com.aistudio.couplebubble.qxztrw.data.remote

import com.aistudio.couplebubble.qxztrw.core.rethrowIfCancellation
import com.aistudio.couplebubble.qxztrw.data.local.ImageUrls
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Puts photos into Firebase Storage under `couples/{coupleId}/{folder}/`. Every upload gets a unique name, so a
 * role swap can never make one partner overwrite the file the other partner's URL points to. When Storage is
 * unavailable or slow (Spark plan, offline), the photo travels as a Base64 data URI inside Firestore instead.
 */
class RemoteImageUploader(
    private val firebase: FirebaseProvider,
    private val uploadTimeout: Duration = 10.seconds
) {

    private companion object {
        const val STORAGE_ROOT = "couples"
        const val STORAGE_URL_PREFIX = "https://firebasestorage.googleapis.com/"
    }

    fun isStorageUrl(url: String?): Boolean = url != null && url.startsWith(STORAGE_URL_PREFIX, ignoreCase = true)

    /** The download URL, or `null` if Storage is unavailable or the upload does not finish in time. */
    suspend fun upload(coupleId: String, folder: String, namePrefix: String, bytes: ByteArray): String? = try {
        firebase.ensureSignedIn()
        firebase.storage?.reference
            ?.child(STORAGE_ROOT)
            ?.child(coupleId)
            ?.child(folder)
            ?.child(uniqueName(namePrefix))
            ?.let { ref ->
                withTimeoutOrNull(uploadTimeout) {
                    ref.putBytes(bytes).await()
                    ref.downloadUrl.await().toString()
                }
            }
    } catch (e: Exception) {
        e.rethrowIfCancellation()
        null
    }

    /** A Storage URL if the upload works, otherwise a Base64 data URI partner devices can still render. */
    suspend fun uploadOrDataUri(coupleId: String, folder: String, namePrefix: String, bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        return upload(coupleId, folder, namePrefix, bytes) ?: dataUri(bytes)
    }

    fun dataUri(bytes: ByteArray): String? = try {
        ImageUrls.jpegDataUri(bytes)
    } catch (_: Exception) {
        null
    }

    /** Deletes a file this app uploaded; other URLs (data URIs, foreign hosts) are ignored. */
    suspend fun delete(url: String?) {
        if (!isStorageUrl(url)) return
        try {
            firebase.storage?.getReferenceFromUrl(requireNotNull(url))?.delete()?.await()
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            // Already gone or not ours
        }
    }

    /** Deletes `couples/{coupleId}/{folder}/{name}`, e.g. files from before uploads got unique names. */
    suspend fun deletePath(coupleId: String, folder: String, name: String) {
        try {
            firebase.storage?.reference?.child(STORAGE_ROOT)?.child(coupleId)?.child(folder)?.child(name)?.delete()?.await()
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            // Ignore if not found
        }
    }

    private fun uniqueName(prefix: String): String = "${prefix}_${firebase.currentUid ?: "anon"}_${System.currentTimeMillis()}.jpg"
}
