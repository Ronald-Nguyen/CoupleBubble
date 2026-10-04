package com.aistudio.couplebubble.qxztrw.repository

import android.content.Context
import androidx.core.net.toUri
import com.aistudio.couplebubble.qxztrw.data.ImageUrls
import com.aistudio.couplebubble.qxztrw.data.LocalImageStorage
import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.PairingCode
import com.aistudio.couplebubble.qxztrw.model.SpaceDefaults
import com.aistudio.couplebubble.qxztrw.model.UserProfile
import com.aistudio.couplebubble.qxztrw.repository.FirestoreSchema.Space
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class FirebaseCoupleRepository(private val appContext: Context) : CoupleRepository {

    private companion object {
        const val DEMO_SPACE_ID = "demo_space"
        const val STORAGE_BUCKET_FALLBACK = "gs://couplebubble-a8139.firebasestorage.app"
        const val STORAGE_ROOT = "couples"
        const val STORAGE_MEMORIES = "memories"
        const val STORAGE_PROFILES = "profiles"
        const val PAIRING_CODE_VALID_SECONDS = 900

        // One transaction holds at most 500 writes: the space document plus every memory
        const val MAX_SWAP_MEMORIES = 499

        // Reads wait this long for the server before falling back to the offline cache
        val CACHE_FIRST_TIMEOUT = 2.seconds
        val SPACE_WRITE_TIMEOUT = 5.seconds
        val UPLOAD_TIMEOUT = 10.seconds
    }

    private val _currentSpace = MutableStateFlow<CoupleSpace?>(null)
    override val currentSpace: StateFlow<CoupleSpace?> = _currentSpace.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    override val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    private var spaceListenerRegistration: ListenerRegistration? = null

    private val db: FirebaseFirestore? by lazy {
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

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private val storage: FirebaseStorage? by lazy {
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

    private fun firestoreUnavailable(): Result<Nothing> = Result.failure(IllegalStateException("Firestore uninitialized"))

    private fun FirebaseFirestore.space(coupleId: String): DocumentReference = collection(FirestoreSchema.SPACES).document(coupleId)

    private fun FirebaseFirestore.memory(coupleId: String, memoryId: String): DocumentReference =
        space(coupleId).collection(FirestoreSchema.MEMORIES).document(memoryId)

    override val currentAuthUid: String?
        get() = try {
            auth?.currentUser?.uid
        } catch (_: Exception) {
            null
        }

    override suspend fun ensureSignedIn(): String? {
        val currentAuth = auth ?: return null
        if (currentAuth.currentUser == null) {
            try {
                currentAuth.signInAnonymously().await()
            } catch (_: Exception) {
                // Stays signed out; Firestore rules reject the writes later
            }
        }
        return currentAuth.currentUser?.uid
    }

    private fun isStorageDownloadUrl(url: String?): Boolean =
        url != null && url.startsWith("https://firebasestorage.googleapis.com/", ignoreCase = true)

    private suspend fun deleteStorageFile(url: String?) {
        if (url == null || !isStorageDownloadUrl(url)) return
        try {
            storage?.getReferenceFromUrl(url)?.delete()?.await()
        } catch (_: Exception) {
            // Already gone or not ours
        }
    }

    private fun storageFileName(prefix: String): String {
        val uid = auth?.currentUser?.uid ?: "anon"
        return "${prefix}_${uid}_${System.currentTimeMillis()}.jpg"
    }

    /**
     * Uploads [bytes] to `couples/{coupleId}/{folder}/` under a unique name and returns the download URL, or
     * `null` if Storage is unavailable or the upload does not finish within [UPLOAD_TIMEOUT].
     */
    private suspend fun uploadJpeg(coupleId: String, folder: String, namePrefix: String, bytes: ByteArray): String? =
        try {
            ensureSignedIn()
            storage?.reference
                ?.child(STORAGE_ROOT)
                ?.child(coupleId)
                ?.child(folder)
                ?.child(storageFileName(namePrefix))
                ?.let { ref ->
                    withTimeoutOrNull(UPLOAD_TIMEOUT) {
                        ref.putBytes(bytes).await()
                        ref.downloadUrl.await().toString()
                    }
                }
        } catch (_: Exception) {
            null
        }

    private fun readLocalFileBytes(fileUri: String?): ByteArray? {
        if (fileUri.isNullOrBlank()) return null
        return try {
            val cleanUri = fileUri.substringBefore("?")
            if (cleanUri.startsWith("content://")) {
                return appContext.contentResolver.openInputStream(cleanUri.toUri())?.use { it.readBytes() }
            }
            val path = when {
                cleanUri.startsWith("file://") -> cleanUri.removePrefix("file://")
                cleanUri.startsWith("/") -> cleanUri
                else -> cleanUri.toUri().path
            } ?: return null
            File(path).takeIf { it.exists() && it.length() > 0 }?.readBytes()
        } catch (_: Exception) {
            null
        }
    }

    private fun listenToSpaceChanges(spaceId: String) {
        spaceListenerRegistration?.remove()
        val firestore = db ?: return

        spaceListenerRegistration = firestore.space(spaceId).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            // An empty offline cache reports the space as missing; only the server may say it is gone
            if (!snapshot.exists() && snapshot.metadata.isFromCache) return@addSnapshotListener
            _currentSpace.value = spaceFromSnapshot(snapshot)
        }
    }

    /** Parses a space document; returns null when it no longer exists or was disconnected. */
    private fun spaceFromSnapshot(snapshot: DocumentSnapshot): CoupleSpace? {
        if (!snapshot.exists() || snapshot.getBoolean(Space.IS_ACTIVE) == false) return null

        val partner1 = snapshot.partner1Name()
        val partner2 = snapshot.partner2Name()

        return CoupleSpace(
            id = snapshot.id,
            partner1Name = partner1,
            partner2Name = partner2,
            anniversaryYear = snapshot.getLong(Space.ANNIVERSARY_YEAR)?.toInt() ?: SpaceDefaults.ANNIVERSARY_YEAR,
            anniversaryMonth = snapshot.getLong(Space.ANNIVERSARY_MONTH)?.toInt() ?: SpaceDefaults.ANNIVERSARY_MONTH,
            anniversaryDay = snapshot.getLong(Space.ANNIVERSARY_DAY)?.toInt() ?: SpaceDefaults.ANNIVERSARY_DAY,
            anniversaryEpochMillis = snapshot.getLong(Space.ANNIVERSARY_EPOCH_MILLIS) ?: SpaceDefaults.ANNIVERSARY_EPOCH_MILLIS,
            isSetupComplete = snapshot.getBoolean(Space.IS_SETUP_COMPLETE)
                ?: (partner1 != SpaceDefaults.PARTNER_1_NAME || partner2 != SpaceDefaults.PARTNER_2_NAME),
            isActive = true,
            userUids = snapshot.userUids(),
            partner1Id = snapshot.getString(Space.PARTNER_1_ID),
            partner2Id = snapshot.getString(Space.PARTNER_2_ID),
            partner1PhotoUrl = snapshot.partner1PhotoUrl(),
            partner2PhotoUrl = snapshot.partner2PhotoUrl(),
            partner1ColorHex = snapshot.partner1ColorHex(),
            partner2ColorHex = snapshot.partner2ColorHex()
        )
    }

    private fun memoryFromSnapshot(doc: DocumentSnapshot): Memory? = try {
        val dateStr = doc.getString(FirestoreSchema.Memory.DATE)
        val date = if (!dateStr.isNullOrEmpty()) LocalDate.parse(dateStr) else LocalDate.now()
        val (partner1Url, partner2Url) = resolveMemorySlots(
            legacyUrl = ImageUrls.sanitize(doc.getString(FirestoreSchema.Memory.LEGACY_IMAGE_URL)),
            partner1Url = ImageUrls.sanitize(doc.getString(FirestoreSchema.Memory.PARTNER_1_IMAGE)),
            partner2Url = ImageUrls.sanitize(doc.getString(FirestoreSchema.Memory.PARTNER_2_IMAGE)),
        )
        Memory(
            id = doc.id,
            title = doc.getString(FirestoreSchema.Memory.TITLE) ?: "",
            date = date,
            note = doc.getString(FirestoreSchema.Memory.NOTE) ?: "",
            partner1ImageUrl = partner1Url,
            partner2ImageUrl = partner2Url
        )
    } catch (_: Exception) {
        null
    }

    override fun getMemories(coupleId: String): Flow<List<Memory>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.space(coupleId).collection(FirestoreSchema.MEMORIES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val memories = snapshot?.documents?.mapNotNull(::memoryFromSnapshot)
                    ?.sortedByDescending { it.date }
                    ?: emptyList()
                trySend(memories)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun addMemory(
        coupleId: String,
        memory: Memory,
        image1Bytes: ByteArray?,
        image2Bytes: ByteArray?
    ): Result<Memory> {
        val firestore = db ?: return firestoreUnavailable()
        val memoryId = memory.id.ifBlank { UUID.randomUUID().toString() }

        val finalUrl1 = persistMemorySlot(coupleId, memoryId, "a", memory.partner1ImageUrl, image1Bytes)
        val finalUrl2 = persistMemorySlot(coupleId, memoryId, "b", memory.partner2ImageUrl, image2Bytes)

        val memoryData = mapOf(
            FirestoreSchema.Memory.ID to memoryId,
            FirestoreSchema.Memory.TITLE to memory.title,
            FirestoreSchema.Memory.DATE to memory.date.toString(),
            FirestoreSchema.Memory.NOTE to memory.note,
            FirestoreSchema.Memory.PARTNER_1_IMAGE to finalUrl1,
            FirestoreSchema.Memory.PARTNER_2_IMAGE to finalUrl2,
            FirestoreSchema.Memory.CREATED_AT to System.currentTimeMillis()
        )

        return try {
            firestore.memory(coupleId, memoryId).set(memoryData).await()
            Result.success(
                memory.copy(id = memoryId, imageUrl = null, partner1ImageUrl = finalUrl1, partner2ImageUrl = finalUrl2)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMemory(
        coupleId: String,
        memory: Memory,
        image1Bytes: ByteArray?,
        image2Bytes: ByteArray?
    ): Result<Memory> {
        val firestore = db ?: return firestoreUnavailable()
        val memoryRef = firestore.memory(coupleId, memory.id)
        val replaces1 = image1Bytes?.isNotEmpty() == true
        val replaces2 = image2Bytes?.isNotEmpty() == true
        val previous = if (replaces1 || replaces2) {
            try {
                withTimeoutOrNull(CACHE_FIRST_TIMEOUT) { memoryRef.get().await() }
            } catch (_: Exception) {
                null
            }
        } else null

        val finalUrl1 = persistMemorySlot(coupleId, memory.id, "a", memory.partner1ImageUrl, image1Bytes)
        val finalUrl2 = persistMemorySlot(coupleId, memory.id, "b", memory.partner2ImageUrl, image2Bytes)

        val memoryData = mapOf(
            FirestoreSchema.Memory.ID to memory.id,
            FirestoreSchema.Memory.TITLE to memory.title,
            FirestoreSchema.Memory.DATE to memory.date.toString(),
            FirestoreSchema.Memory.NOTE to memory.note,
            FirestoreSchema.Memory.LEGACY_IMAGE_URL to FieldValue.delete(),
            FirestoreSchema.Memory.PARTNER_1_IMAGE to finalUrl1,
            FirestoreSchema.Memory.PARTNER_2_IMAGE to finalUrl2,
            FirestoreSchema.Memory.UPDATED_AT to System.currentTimeMillis()
        )

        return try {
            withTimeoutOrNull(UPLOAD_TIMEOUT) {
                memoryRef.set(memoryData, SetOptions.merge()).await()
            }
            val keptUrls = setOf(finalUrl1, finalUrl2)
            if (replaces1) previous?.getString(FirestoreSchema.Memory.PARTNER_1_IMAGE)?.takeIf { it !in keptUrls }?.let { deleteStorageFile(it) }
            if (replaces2) previous?.getString(FirestoreSchema.Memory.PARTNER_2_IMAGE)?.takeIf { it !in keptUrls }?.let { deleteStorageFile(it) }
            Result.success(memory.copy(imageUrl = null, partner1ImageUrl = finalUrl1, partner2ImageUrl = finalUrl2))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns the Firestore-safe URL for one memory photo slot. New bytes are uploaded under a unique
     * Storage path (never shared between slots, so a role swap can't make one partner overwrite the
     * other's file), falling back to a Base64 data URI. Local sandbox URIs are never returned.
     */
    private suspend fun persistMemorySlot(
        coupleId: String,
        memoryId: String,
        slot: String,
        currentUrl: String?,
        bytes: ByteArray?
    ): String? {
        val url = ImageUrls.sanitize(currentUrl)
        val effectiveBytes = bytes?.takeIf { it.isNotEmpty() } ?: if (ImageUrls.isLocalUri(url)) readLocalFileBytes(url) else null
        if (effectiveBytes == null || effectiveBytes.isEmpty()) {
            return if (ImageUrls.isLocalUri(url)) null else url
        }

        return uploadJpeg(coupleId, STORAGE_MEMORIES, "${memoryId}_$slot", effectiveBytes) ?: try {
            ImageUrls.jpegDataUri(effectiveBytes)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun deleteMemory(coupleId: String, memoryId: String): Result<Unit> {
        val firestore = db ?: return firestoreUnavailable()
        val memoryRef = firestore.memory(coupleId, memoryId)
        return try {
            val snapshot = try {
                withTimeoutOrNull(CACHE_FIRST_TIMEOUT) { memoryRef.get().await() }
            } catch (_: Exception) {
                null
            }
            listOf(
                FirestoreSchema.Memory.PARTNER_1_IMAGE,
                FirestoreSchema.Memory.PARTNER_2_IMAGE,
                FirestoreSchema.Memory.LEGACY_IMAGE_URL
            )
                .mapNotNull { snapshot?.getString(it) }
                .distinct()
                .forEach { deleteStorageFile(it) }
            // Fixed slot paths used before uploads got unique names
            for (legacyName in listOf("${memoryId}_a.jpg", "${memoryId}_b.jpg")) {
                try {
                    storage?.reference
                        ?.child(STORAGE_ROOT)
                        ?.child(coupleId)
                        ?.child(STORAGE_MEMORIES)
                        ?.child(legacyName)
                        ?.delete()
                        ?.await()
                } catch (_: Exception) {
                    // Ignore if not found
                }
            }

            withTimeoutOrNull(CACHE_FIRST_TIMEOUT) {
                memoryRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCustomMilestones(coupleId: String): Flow<List<CustomMilestone>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.space(coupleId).collection(FirestoreSchema.MILESTONES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val milestones = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        CustomMilestone(
                            id = doc.id,
                            title = doc.getString("title") ?: return@mapNotNull null,
                            date = LocalDate.parse(doc.getString("date") ?: return@mapNotNull null)
                        )
                    } catch (_: Exception) {
                        null
                    }
                }?.sortedBy { it.date } ?: emptyList()

                trySend(milestones)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun addCustomMilestone(coupleId: String, milestone: CustomMilestone): Result<CustomMilestone> {
        val firestore = db ?: return firestoreUnavailable()
        val milestoneId = milestone.id.ifBlank { UUID.randomUUID().toString() }
        return try {
            val write = firestore.space(coupleId)
                .collection(FirestoreSchema.MILESTONES)
                .document(milestoneId)
                .set(
                    mapOf(
                        "title" to milestone.title,
                        "date" to milestone.date.toString(),
                        "createdAt" to System.currentTimeMillis()
                    )
                )
            // The offline cache applies the write immediately; don't block on the server ack
            withTimeoutOrNull(CACHE_FIRST_TIMEOUT) { write.await() }
            Result.success(milestone.copy(id = milestoneId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCustomMilestone(coupleId: String, milestoneId: String): Result<Unit> {
        val firestore = db ?: return firestoreUnavailable()
        return try {
            val delete = firestore.space(coupleId)
                .collection(FirestoreSchema.MILESTONES)
                .document(milestoneId)
                .delete()
            withTimeoutOrNull(CACHE_FIRST_TIMEOUT) { delete.await() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePartnerNames(
        coupleId: String,
        partner1Name: String,
        partner2Name: String
    ): Result<Unit> {
        val firestore = db ?: return firestoreUnavailable()
        return try {
            firestore.space(coupleId).update(
                mapOf(
                    Space.PARTNER_1_NAME to partner1Name,
                    Space.PARTNER_2_NAME to partner2Name,
                    Space.PARTNER_1_NAME_ALIAS to partner1Name,
                    Space.PARTNER_2_NAME_ALIAS to partner2Name,
                    Space.IS_SETUP_COMPLETE to true
                )
            ).await()
            _currentSpace.value = _currentSpace.value?.copy(
                partner1Name = partner1Name,
                partner2Name = partner2Name,
                isSetupComplete = true
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSpaceDetails(
        coupleId: String,
        partner1Name: String,
        partner2Name: String,
        anniversaryYear: Int,
        anniversaryMonth: Int,
        anniversaryDay: Int
    ): Result<Unit> {
        val firestore = db
        val localDate = LocalDate.of(anniversaryYear, anniversaryMonth, anniversaryDay)
        val epochMillis = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (firestore != null) {
            try {
                val updateData = mapOf<String, Any>(
                    Space.PARTNER_1_NAME to partner1Name,
                    Space.PARTNER_2_NAME to partner2Name,
                    Space.PARTNER_1_NAME_ALIAS to partner1Name,
                    Space.PARTNER_2_NAME_ALIAS to partner2Name,
                    Space.ANNIVERSARY_YEAR to anniversaryYear,
                    Space.ANNIVERSARY_MONTH to anniversaryMonth,
                    Space.ANNIVERSARY_DAY to anniversaryDay,
                    Space.ANNIVERSARY_EPOCH_MILLIS to epochMillis,
                    Space.IS_SETUP_COMPLETE to true,
                    Space.UPDATED_AT to System.currentTimeMillis()
                )

                firestore.space(coupleId).set(updateData, SetOptions.merge()).await()

                if (auth?.currentUser != null) {
                    linkCurrentUserToSpace(coupleId)
                }
            } catch (_: Exception) {
                // Ignore network error and update locally
            }
        }

        _currentSpace.value = _currentSpace.value?.copy(
            partner1Name = partner1Name,
            partner2Name = partner2Name,
            anniversaryYear = anniversaryYear,
            anniversaryMonth = anniversaryMonth,
            anniversaryDay = anniversaryDay,
            anniversaryEpochMillis = epochMillis,
            isSetupComplete = true
        )
        return Result.success(Unit)
    }

    private fun normalizePairingCode(code: String): String = code.replace("-", "").trim().uppercase()

    private fun spaceIdForCode(cleanCode: String): String = "space_$cleanCode"

    override suspend fun listenToPairingCode(code: String) {
        val cleanCode = normalizePairingCode(code)
        if (cleanCode.length != 6) return
        try {
            val firestore = db ?: return
            val spaceId = spaceIdForCode(cleanCode)
            val spaceDocRef = firestore.space(spaceId)
            val snapshot = withTimeoutOrNull(CACHE_FIRST_TIMEOUT) { spaceDocRef.get().await() }
            if (snapshot == null || !snapshot.exists()) {
                val spaceData = mapOf(
                    Space.ID to spaceId,
                    Space.PAIRING_CODE to cleanCode,
                    Space.PARTNER_1_NAME to SpaceDefaults.PARTNER_1_NAME,
                    Space.PARTNER_2_NAME to SpaceDefaults.PARTNER_2_NAME,
                    Space.ANNIVERSARY_YEAR to SpaceDefaults.ANNIVERSARY_YEAR,
                    Space.ANNIVERSARY_MONTH to SpaceDefaults.ANNIVERSARY_MONTH,
                    Space.ANNIVERSARY_DAY to SpaceDefaults.ANNIVERSARY_DAY,
                    Space.IS_SETUP_COMPLETE to false,
                    Space.IS_ACTIVE to false,
                    Space.CREATED_AT to System.currentTimeMillis()
                )
                withTimeoutOrNull(CACHE_FIRST_TIMEOUT) { spaceDocRef.set(spaceData).await() }
            }
            listenToSpaceChanges(spaceId)
        } catch (_: Exception) {
            // Ignore if Firebase uninitialized
        }
    }

    override suspend fun generateNewPairingCode(): PairingCode {
        val digits = (100000..999999).random().toString()
        val generatedCode = "${digits.take(3)}-${digits.drop(3)}"

        listenToPairingCode(generatedCode)

        return PairingCode(code = generatedCode, totalValidSeconds = PAIRING_CODE_VALID_SECONDS)
    }

    override suspend fun connectWithCode(code: String): Result<CoupleSpace> {
        val cleanCode = normalizePairingCode(code)

        if (cleanCode.length != 6 || !cleanCode.all { it in '0'..'9' }) {
            return Result.failure(InvalidPairingCodeException())
        }

        try {
            val firestore = db
            if (firestore != null) {
                var snapshot = withTimeoutOrNull(CACHE_FIRST_TIMEOUT) {
                    firestore.space(spaceIdForCode(cleanCode)).get().await()
                }

                if (snapshot == null || !snapshot.exists()) {
                    val query = withTimeoutOrNull(CACHE_FIRST_TIMEOUT) {
                        firestore.collection(FirestoreSchema.SPACES)
                            .whereEqualTo(Space.PAIRING_CODE, cleanCode)
                            .get()
                            .await()
                    }
                    if (query != null && !query.isEmpty) {
                        snapshot = query.documents.first()
                    }
                }

                val existing = snapshot?.takeIf { it.exists() }
                val members = (existing?.get(Space.USER_UIDS) as? List<*>)
                    ?: (existing?.get(Space.MEMBERS) as? List<*>)
                    ?: emptyList<Any>()

                if (existing != null) {
                    val partner2Id = existing.getString(Space.PARTNER_2_ID)
                    val currentUid = auth?.currentUser?.uid

                    val isAlreadyMember = currentUid != null && (
                        currentUid in members ||
                            currentUid == existing.getString(Space.PARTNER_1_ID) ||
                            currentUid == partner2Id
                        )
                    val isRoomFull = members.size >= 2 || (!partner2Id.isNullOrBlank() && partner2Id != currentUid)

                    if (isRoomFull && !isAlreadyMember) {
                        return Result.failure(SpaceFullException())
                    }
                }

                // A snapshot that does not exist carries no fields, so all values below fall back to the defaults
                val docId = existing?.id ?: spaceIdForCode(cleanCode)
                val partner1 = existing?.partner1Name() ?: SpaceDefaults.PARTNER_1_NAME
                val partner2 = existing?.partner2Name() ?: SpaceDefaults.PARTNER_2_NAME
                val isSetupComplete = existing?.getBoolean(Space.IS_SETUP_COMPLETE) ?: false
                val anniversaryYear = existing?.getLong(Space.ANNIVERSARY_YEAR)?.toInt() ?: SpaceDefaults.ANNIVERSARY_YEAR
                val anniversaryMonth = existing?.getLong(Space.ANNIVERSARY_MONTH)?.toInt() ?: SpaceDefaults.ANNIVERSARY_MONTH
                val anniversaryDay = existing?.getLong(Space.ANNIVERSARY_DAY)?.toInt() ?: SpaceDefaults.ANNIVERSARY_DAY

                withTimeoutOrNull(CACHE_FIRST_TIMEOUT) {
                    firestore.space(docId).set(
                        mapOf(
                            Space.ID to docId,
                            Space.PAIRING_CODE to cleanCode,
                            Space.PARTNER_1_NAME to partner1,
                            Space.PARTNER_2_NAME to partner2,
                            Space.PARTNER_1_NAME_ALIAS to partner1,
                            Space.PARTNER_2_NAME_ALIAS to partner2,
                            Space.ANNIVERSARY_YEAR to anniversaryYear,
                            Space.ANNIVERSARY_MONTH to anniversaryMonth,
                            Space.ANNIVERSARY_DAY to anniversaryDay,
                            Space.IS_SETUP_COMPLETE to isSetupComplete,
                            Space.IS_ACTIVE to true,
                            Space.PAIRED_AT to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    ).await()
                }

                val pairedSpace = CoupleSpace(
                    id = docId,
                    partner1Name = partner1,
                    partner2Name = partner2,
                    anniversaryYear = anniversaryYear,
                    anniversaryMonth = anniversaryMonth,
                    anniversaryDay = anniversaryDay,
                    isSetupComplete = isSetupComplete,
                    isActive = true,
                    userUids = members.filterIsInstance<String>(),
                    partner1Id = existing?.getString(Space.PARTNER_1_ID),
                    partner2Id = existing?.getString(Space.PARTNER_2_ID),
                    partner1PhotoUrl = existing?.getString(Space.PARTNER_1_PHOTO),
                    partner2PhotoUrl = existing?.getString(Space.PARTNER_2_PHOTO),
                    partner1ColorHex = existing?.partner1ColorHex() ?: SpaceDefaults.PARTNER_1_COLOR_HEX,
                    partner2ColorHex = existing?.partner2ColorHex() ?: SpaceDefaults.PARTNER_2_COLOR_HEX
                )
                _currentSpace.value = pairedSpace
                listenToSpaceChanges(pairedSpace.id)

                val currentUser = auth?.currentUser
                if (currentUser != null) {
                    claimPartnerSlot(pairedSpace.id, currentUser.uid, preferPartner2 = true)
                    linkCurrentUserToSpace(pairedSpace.id)
                }

                return Result.success(pairedSpace)
            }
        } catch (e: IllegalStateException) {
            return Result.failure(e)
        } catch (_: Exception) {
            // Fallback if network or Firebase unavailable
        }

        return Result.failure(SpaceUnavailableException())
    }

    override suspend fun restoreSession(coupleId: String): Result<CoupleSpace> {
        val firestore = db ?: return Result.failure(SpaceUnavailableException())
        val docRef = firestore.space(coupleId)
        // A slow start must not fall back to placeholder names: the offline cache holds the last known space
        val doc = try {
            withTimeoutOrNull(CACHE_FIRST_TIMEOUT) { docRef.get().await() }
        } catch (_: Exception) {
            null
        } ?: try {
            docRef.get(Source.CACHE).await()
        } catch (_: Exception) {
            null
        }

        if (doc == null || (!doc.exists() && doc.metadata.isFromCache)) {
            // Not loaded yet; the listener fills in the space as soon as Firestore answers
            listenToSpaceChanges(coupleId)
            return Result.failure(SpaceUnavailableException())
        }

        val space = spaceFromSnapshot(doc)
        if (space == null) {
            _currentSpace.value = null
            return Result.failure(IllegalStateException("Couple space is inactive"))
        }
        _currentSpace.value = space
        listenToSpaceChanges(space.id)
        auth?.currentUser?.let { user ->
            if (space.id != DEMO_SPACE_ID && user.uid != space.partner1Id && user.uid != space.partner2Id) {
                claimPartnerSlot(space.id, user.uid, preferPartner2 = false)
            }
            if (_currentUserProfile.value == null) {
                _currentUserProfile.value = UserProfile(
                    uid = user.uid,
                    email = user.email,
                    displayName = user.displayName,
                    coupleId = space.id
                )
            }
        }
        return Result.success(space)
    }

    override suspend fun restoreSessionForUser(uid: String): Result<CoupleSpace?> {
        val firestore = db ?: return Result.success(null)
        try {
            val userDoc = withTimeoutOrNull(CACHE_FIRST_TIMEOUT) {
                firestore.collection(FirestoreSchema.USERS).document(uid).get().await()
            }
            if (userDoc != null && userDoc.exists()) {
                val coupleId = userDoc.getString(FirestoreSchema.User.COUPLE_ID)
                _currentUserProfile.value = UserProfile(
                    uid = uid,
                    email = userDoc.getString(FirestoreSchema.User.EMAIL),
                    displayName = userDoc.getString(FirestoreSchema.User.DISPLAY_NAME),
                    coupleId = coupleId
                )

                if (!coupleId.isNullOrBlank()) {
                    return Result.success(restoreSession(coupleId).getOrNull())
                }
            }
        } catch (_: Exception) {
            // Ignore error on restore
        }
        return Result.success(null)
    }

    override suspend fun disconnect() {
        spaceListenerRegistration?.remove()
        spaceListenerRegistration = null
        _currentSpace.value = null
    }

    override suspend fun disconnectCouple(coupleId: String): Result<Unit> {
        try {
            db?.space(coupleId)?.update(
                mapOf(
                    Space.IS_ACTIVE to false,
                    Space.DISCONNECTED_AT to System.currentTimeMillis()
                )
            )?.await()
        } catch (_: Exception) {
            // Ignore if Firebase uninitialized
        }
        disconnect()
        return Result.success(Unit)
    }

    override suspend fun openDemoSpace(): CoupleSpace {
        val demoSpace = CoupleSpace(
            id = DEMO_SPACE_ID,
            isSetupComplete = true,
            isActive = true
        )

        try {
            withTimeoutOrNull(CACHE_FIRST_TIMEOUT) {
                db?.space(DEMO_SPACE_ID)?.set(
                    mapOf(
                        Space.ID to demoSpace.id,
                        Space.PARTNER_1_NAME to demoSpace.partner1Name,
                        Space.PARTNER_2_NAME to demoSpace.partner2Name,
                        Space.PARTNER_1_NAME_ALIAS to demoSpace.partner1Name,
                        Space.PARTNER_2_NAME_ALIAS to demoSpace.partner2Name,
                        Space.ANNIVERSARY_YEAR to demoSpace.anniversaryYear,
                        Space.ANNIVERSARY_MONTH to demoSpace.anniversaryMonth,
                        Space.ANNIVERSARY_DAY to demoSpace.anniversaryDay,
                        Space.IS_SETUP_COMPLETE to true,
                        Space.IS_ACTIVE to true
                    )
                )?.await()
            }
        } catch (_: Exception) {
            // Ignore if Firebase uninitialized
        }

        _currentSpace.value = demoSpace
        listenToSpaceChanges(demoSpace.id)
        return demoSpace
    }

    override suspend fun signInWithGoogleUser(
        uid: String,
        email: String?,
        displayName: String?,
        previousUid: String?
    ): Result<UserProfile> {
        val firestore = db
        var coupleId: String? = _currentSpace.value?.id

        if (firestore != null) {
            try {
                val userDocRef = firestore.collection(FirestoreSchema.USERS).document(uid)
                val snapshot = withTimeoutOrNull(CACHE_FIRST_TIMEOUT) { userDocRef.get().await() }

                if (snapshot != null && snapshot.exists()) {
                    val existingCoupleId = snapshot.getString(FirestoreSchema.User.COUPLE_ID)
                    if (!existingCoupleId.isNullOrBlank()) {
                        coupleId = existingCoupleId
                        restoreSession(existingCoupleId)
                    }
                }

                val userData = mutableMapOf<String, Any?>(
                    FirestoreSchema.User.UID to uid,
                    FirestoreSchema.User.EMAIL to email,
                    FirestoreSchema.User.DISPLAY_NAME to displayName,
                    FirestoreSchema.User.UPDATED_AT to System.currentTimeMillis()
                )
                if (!coupleId.isNullOrBlank()) {
                    userData[FirestoreSchema.User.COUPLE_ID] = coupleId
                }

                userDocRef.set(userData, SetOptions.merge()).await()

                if (!coupleId.isNullOrBlank() && coupleId != DEMO_SPACE_ID) {
                    claimPartnerSlot(coupleId, uid, preferPartner2 = false, replaceUid = previousUid)
                }
            } catch (_: Exception) {
                // Ignore network error and keep local state
            }
        }

        val profile = UserProfile(uid = uid, email = email, displayName = displayName, coupleId = coupleId)
        _currentUserProfile.value = profile
        return Result.success(profile)
    }

    override suspend fun linkCurrentUserToSpace(coupleId: String): Result<Unit> {
        val user = auth?.currentUser ?: return Result.success(Unit)
        val firestore = db ?: return Result.success(Unit)

        try {
            firestore.collection(FirestoreSchema.USERS).document(user.uid).set(
                mapOf(
                    FirestoreSchema.User.UID to user.uid,
                    FirestoreSchema.User.EMAIL to user.email,
                    FirestoreSchema.User.DISPLAY_NAME to user.displayName,
                    FirestoreSchema.User.COUPLE_ID to coupleId,
                    FirestoreSchema.User.UPDATED_AT to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()

            claimPartnerSlot(coupleId, user.uid, preferPartner2 = false)

            _currentUserProfile.value = UserProfile(
                uid = user.uid,
                email = user.email,
                displayName = user.displayName,
                coupleId = coupleId
            )
        } catch (_: Exception) {
            // Ignore error if network fails
        }
        return Result.success(Unit)
    }

    private fun CoupleSpace.withPartnerPhoto(isPartner1: Boolean, url: String): CoupleSpace =
        if (isPartner1) copy(partner1PhotoUrl = url) else copy(partner2PhotoUrl = url)

    override suspend fun uploadProfilePhoto(
        coupleId: String,
        isPartner1: Boolean,
        imageBytes: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        val previousSpace = _currentSpace.value
        val previousUrl = if (isPartner1) previousSpace?.partner1PhotoUrl else previousSpace?.partner2PhotoUrl
        val partnerUrl = if (isPartner1) previousSpace?.partner2PhotoUrl else previousSpace?.partner1PhotoUrl

        // 1. Save locally for immediate display and offline resilience
        val localUrl = LocalImageStorage.saveProfilePhoto(appContext, coupleId, isPartner1, imageBytes)
        if (localUrl != null) {
            _currentSpace.value = _currentSpace.value?.withPartnerPhoto(isPartner1, localUrl)
        }

        // 2. Prefer a Storage URL; the Base64 data URI keeps partner devices in sync if the upload fails.
        // The Storage path is unique per upload: a fixed per-slot path would, after a role swap, overwrite
        // the file the partner's URL still points to.
        val base64DataUri = try {
            ImageUrls.jpegDataUri(imageBytes)
        } catch (_: Exception) {
            null
        }
        val syncUrl = uploadJpeg(coupleId, STORAGE_PROFILES, "profile", imageBytes) ?: base64DataUri

        // 3. Always update Firestore so partner devices receive the photo immediately
        val firestore = db
        if (syncUrl != null && firestore != null) {
            try {
                val fieldName = if (isPartner1) Space.PARTNER_1_PHOTO else Space.PARTNER_2_PHOTO
                val aliasFieldName = if (isPartner1) Space.PARTNER_1_PHOTO_ALIAS else Space.PARTNER_2_PHOTO_ALIAS

                withTimeoutOrNull(SPACE_WRITE_TIMEOUT) {
                    firestore.space(coupleId).set(
                        mapOf(
                            fieldName to syncUrl,
                            aliasFieldName to syncUrl,
                            Space.UPDATED_AT to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    ).await()
                }

                _currentSpace.value = _currentSpace.value?.withPartnerPhoto(isPartner1, syncUrl)
                if (previousUrl != syncUrl && previousUrl != partnerUrl) {
                    deleteStorageFile(previousUrl)
                }
            } catch (_: Exception) {
                // Firestore sync delayed; local state preserved
            }
        }

        val finalUrl = syncUrl ?: localUrl
        if (!finalUrl.isNullOrBlank()) {
            Result.success(finalUrl)
        } else {
            Result.failure(IllegalStateException("Profile photo could not be saved"))
        }
    }

    override suspend fun updatePartnerColor(
        coupleId: String,
        isPartner1: Boolean,
        colorHex: String
    ): Result<Unit> {
        val firestore = db ?: return firestoreUnavailable()
        val fieldName = if (isPartner1) Space.PARTNER_1_COLOR else Space.PARTNER_2_COLOR
        val aliasFieldName = if (isPartner1) Space.PARTNER_1_COLOR_ALIAS else Space.PARTNER_2_COLOR_ALIAS

        return try {
            firestore.space(coupleId).set(
                mapOf(
                    fieldName to colorHex,
                    aliasFieldName to colorHex,
                    Space.UPDATED_AT to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()

            _currentSpace.value = if (isPartner1) {
                _currentSpace.value?.copy(partner1ColorHex = colorHex)
            } else {
                _currentSpace.value?.copy(partner2ColorHex = colorHex)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Binds [uid] to an explicit partner slot (`partner1Id` / `partner2Id`) and keeps `userUids` ordered
     * as [partner1, partner2]. Roles are decided by these IDs, not by array order, so `arrayUnion`
     * appends can no longer flip who is who. Spaces from before the IDs existed are backfilled from
     * the old `userUids` order. [replaceUid] swaps an old UID (e.g. anonymous before Google sign-in) for [uid].
     */
    private suspend fun claimPartnerSlot(
        coupleId: String,
        uid: String,
        preferPartner2: Boolean,
        replaceUid: String? = null
    ) {
        val firestore = db ?: return
        val spaceRef = firestore.space(coupleId)
        try {
            withTimeoutOrNull(SPACE_WRITE_TIMEOUT) {
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(spaceRef)
                    if (!snapshot.exists()) return@runTransaction
                    val storedUids = snapshot.userUids()
                    var p1 = snapshot.getString(Space.PARTNER_1_ID)
                    var p2 = snapshot.getString(Space.PARTNER_2_ID)
                    if (p1 == null && p2 == null) {
                        p1 = storedUids.getOrNull(0)
                        p2 = storedUids.getOrNull(1)
                    }
                    if (replaceUid != null && replaceUid != uid) {
                        if (p1 == replaceUid) p1 = uid
                        if (p2 == replaceUid) p2 = uid
                    }
                    if (uid != p1 && uid != p2) {
                        if (preferPartner2) {
                            if (p2 == null) p2 = uid else if (p1 == null) p1 = uid
                        } else {
                            if (p1 == null) p1 = uid else if (p2 == null) p2 = uid
                        }
                    }
                    if (uid != p1 && uid != p2) return@runTransaction // Room is full

                    val newUids = listOfNotNull(p1, p2).distinct()
                    if (p1 != snapshot.getString(Space.PARTNER_1_ID) ||
                        p2 != snapshot.getString(Space.PARTNER_2_ID) ||
                        newUids != storedUids
                    ) {
                        transaction.update(
                            spaceRef,
                            mapOf(
                                Space.PARTNER_1_ID to (p1 ?: FieldValue.delete()),
                                Space.PARTNER_2_ID to (p2 ?: FieldValue.delete()),
                                Space.USER_UIDS to newUids
                            )
                        )
                    }
                }.await()
            }
        } catch (_: Exception) {
            // Offline: transactions need the server; retried on next restore/sign-in
        }
    }

    override suspend fun swapPartners(coupleId: String): Result<Unit> {
        val firestore = db ?: return firestoreUnavailable()

        return try {
            val spaceRef = firestore.space(coupleId)
            val memoryRefs = spaceRef.collection(FirestoreSchema.MEMORIES).get().await().documents.map { it.reference }
            if (memoryRefs.size > MAX_SWAP_MEMORIES) {
                return Result.failure(IllegalStateException("Too many memories to swap atomically"))
            }

            // Space and every memory swap in one transaction: either everything changes or nothing does.
            val swapped = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(spaceRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("Space document does not exist")
                }
                val memorySnapshots = memoryRefs.map { transaction.get(it) }

                val userUids = snapshot.userUids()
                val hasExplicitIds = snapshot.getString(Space.PARTNER_1_ID) != null || snapshot.getString(Space.PARTNER_2_ID) != null
                val p1Id = if (hasExplicitIds) snapshot.getString(Space.PARTNER_1_ID) else userUids.getOrNull(0)
                val p2Id = if (hasExplicitIds) snapshot.getString(Space.PARTNER_2_ID) else userUids.getOrNull(1)
                // With only one known member the swap would move their data to slot 2 while they stay partner 1
                if ((p1Id == null) != (p2Id == null)) {
                    throw PartnerNotConnectedException()
                }

                val p1Name = snapshot.partner1Name()
                val p2Name = snapshot.partner2Name()
                val p1Color = snapshot.partner1ColorHex()
                val p2Color = snapshot.partner2ColorHex()

                // Missing values must be deleted, not skipped, or the old photo would stay in both slots
                val newP1Photo: Any = snapshot.partner2PhotoUrl() ?: FieldValue.delete()
                val newP2Photo: Any = snapshot.partner1PhotoUrl() ?: FieldValue.delete()
                val spaceUpdate = mutableMapOf<String, Any>(
                    Space.PARTNER_1_NAME to p2Name,
                    Space.PARTNER_2_NAME to p1Name,
                    Space.PARTNER_1_NAME_ALIAS to p2Name,
                    Space.PARTNER_2_NAME_ALIAS to p1Name,
                    Space.PARTNER_1_PHOTO to newP1Photo,
                    Space.PARTNER_2_PHOTO to newP2Photo,
                    Space.PARTNER_1_PHOTO_ALIAS to newP1Photo,
                    Space.PARTNER_2_PHOTO_ALIAS to newP2Photo,
                    Space.PARTNER_1_COLOR to p2Color,
                    Space.PARTNER_2_COLOR to p1Color,
                    Space.PARTNER_1_COLOR_ALIAS to p2Color,
                    Space.PARTNER_2_COLOR_ALIAS to p1Color,
                    Space.UPDATED_AT to System.currentTimeMillis()
                )
                if (p1Id != null && p2Id != null) {
                    spaceUpdate[Space.PARTNER_1_ID] = p2Id
                    spaceUpdate[Space.PARTNER_2_ID] = p1Id
                    spaceUpdate[Space.USER_UIDS] = listOf(p2Id, p1Id)
                }
                transaction.update(spaceRef, spaceUpdate)

                for (memory in memorySnapshots) {
                    if (!memory.exists()) continue
                    val (slot1, slot2) = resolveMemorySlots(
                        legacyUrl = memory.getString(FirestoreSchema.Memory.LEGACY_IMAGE_URL),
                        partner1Url = memory.getString(FirestoreSchema.Memory.PARTNER_1_IMAGE),
                        partner2Url = memory.getString(FirestoreSchema.Memory.PARTNER_2_IMAGE),
                    )
                    transaction.update(
                        memory.reference,
                        mapOf(
                            FirestoreSchema.Memory.PARTNER_1_IMAGE to (slot2 ?: FieldValue.delete()),
                            FirestoreSchema.Memory.PARTNER_2_IMAGE to (slot1 ?: FieldValue.delete()),
                            FirestoreSchema.Memory.LEGACY_IMAGE_URL to FieldValue.delete()
                        )
                    )
                }
                listOfNotNull(p2Id, p1Id)
            }.await()

            _currentSpace.value?.let { current ->
                _currentSpace.value = current.copy(
                    partner1Name = current.partner2Name,
                    partner2Name = current.partner1Name,
                    partner1PhotoUrl = current.partner2PhotoUrl,
                    partner2PhotoUrl = current.partner1PhotoUrl,
                    partner1ColorHex = current.partner2ColorHex,
                    partner2ColorHex = current.partner1ColorHex,
                    partner1Id = swapped.getOrNull(0),
                    partner2Id = swapped.getOrNull(1),
                    userUids = swapped.ifEmpty { current.userUids }
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.unwrapPartnerNotConnected())
        }
    }

    override suspend fun signOutUser() {
        try {
            auth?.signOut()
        } catch (_: Exception) {
            // Ignore
        }
        _currentUserProfile.value = null
    }
}
