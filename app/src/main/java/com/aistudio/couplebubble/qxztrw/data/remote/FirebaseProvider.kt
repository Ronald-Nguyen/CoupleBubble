package com.aistudio.couplebubble.qxztrw.data.remote

import com.aistudio.couplebubble.qxztrw.core.rethrowIfCancellation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * The Firebase entry points shared by all repositories. Each is `null` when Firebase is not configured (unit
 * tests, builds without `google-services.json`), so callers degrade instead of crashing. Firestore gets its
 * persistent offline cache here, before any repository touches it.
 */
class FirebaseProvider {

    private companion object {
        const val STORAGE_BUCKET_FALLBACK = "gs://couplebubble-a8139.firebasestorage.app"
    }

    val firestore: FirebaseFirestore? by lazy {
        try {
            val firestore = FirebaseFirestore.getInstance()
            try {
                firestore.firestoreSettings = firestoreSettings {
                    setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                }
            } catch (_: Exception) {
                // Settings can only be set before any other Firestore operations
            }
            firestore
        } catch (_: Exception) {
            null
        }
    }

    val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    val storage: FirebaseStorage? by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (_: Exception) {
            try {
                FirebaseStorage.getInstance(STORAGE_BUCKET_FALLBACK)
            } catch (_: Exception) {
                null
            }
        }
    }

    val currentUid: String?
        get() = try {
            auth?.currentUser?.uid
        } catch (_: Exception) {
            null
        }

    /** Signs in anonymously if nobody is signed in yet; returns the resulting UID, if any. */
    suspend fun ensureSignedIn(): String? {
        val currentAuth = auth ?: return null
        if (currentAuth.currentUser == null) {
            try {
                currentAuth.signInAnonymously().await()
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                // Stays signed out; Firestore rules reject the writes later
            }
        }
        return currentAuth.currentUser?.uid
    }
}
