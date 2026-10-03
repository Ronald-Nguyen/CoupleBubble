package com.aistudio.couplebubble.qxztrw.repository

import androidx.core.net.toUri
import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.PairingCode
import com.aistudio.couplebubble.qxztrw.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class FirebaseCoupleRepository : CoupleRepository {

    private companion object {
        const val DEMO_SPACE_ID = "demo_space"

        // One transaction holds at most 500 writes: the space document plus every memory
        const val MAX_SWAP_MEMORIES = 499
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
                val settings = firestoreSettings {
                    setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                }
                firestore.firestoreSettings = settings
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
                FirebaseStorage.getInstance("gs://couplebubble-a8139.firebasestorage.app")
            } catch (_: Exception) {
                null
            }
        }
    }

    internal fun sanitizeImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("file:///9j") -> "data:image/jpeg;base64,${trimmed.removePrefix("file://")}"
            trimmed.startsWith("file://iVBORw0KGgo") -> "data:image/png;base64,${trimmed.removePrefix("file://")}"
            trimmed.startsWith("file://UklGR") -> "data:image/webp;base64,${trimmed.removePrefix("file://")}"
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true) ||
            trimmed.startsWith("content://", ignoreCase = true) ||
            trimmed.startsWith("data:image/", ignoreCase = true) -> trimmed
            trimmed.startsWith("data:", ignoreCase = true) && trimmed.contains("base64,") -> trimmed
            trimmed.startsWith("/9j") -> "data:image/jpeg;base64,$trimmed"
            trimmed.startsWith("iVBORw0KGgo") -> "data:image/png;base64,$trimmed"
            trimmed.startsWith("UklGR") -> "data:image/webp;base64,$trimmed"
            trimmed.startsWith("/") -> "file://$trimmed"
            trimmed.length > 50 && !trimmed.contains(" ") && !trimmed.contains("://") -> "data:image/jpeg;base64,$trimmed"
            else -> null
        }
    }

    /**
     * Maps stored memory fields to (A, B) photo slots.
     * The legacy `imageUrl` used to mirror `A ?: B`, so it only counts as A for old single-photo moments,
     * and an A slot that equals B is a copy produced by that mirror.
     */
    internal fun resolveMemorySlots(legacyUrl: String?, partnerAUrl: String?, partnerBUrl: String?): Pair<String?, String?> {
        val a = partnerAUrl ?: legacyUrl.takeIf { partnerBUrl == null }
        return (if (a != null && a == partnerBUrl) null else a) to partnerBUrl
    }

    private fun isStorageDownloadUrl(url: String?): Boolean =
        url != null && url.startsWith("https://firebasestorage.googleapis.com/", ignoreCase = true)

    private suspend fun deleteStorageFile(url: String?) {
        if (!isStorageDownloadUrl(url)) return
        try {
            storage?.getReferenceFromUrl(url!!)?.delete()?.await()
        } catch (_: Exception) {
            // Already gone or not ours
        }
    }

    private fun storageFileName(prefix: String): String {
        val uid = auth?.currentUser?.uid ?: "anon"
        return "${prefix}_${uid}_${System.currentTimeMillis()}.jpg"
    }

    internal fun isLocalUri(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        if (trimmed.startsWith("file:///9j") || trimmed.startsWith("file://iVBORw0KGgo") || trimmed.startsWith("file://UklGR")) return false
        return (trimmed.startsWith("file://", ignoreCase = true) ||
                trimmed.startsWith("content://", ignoreCase = true) ||
                (trimmed.startsWith("/") && !trimmed.startsWith("/9j")))
    }

    private fun readLocalFileBytes(fileUri: String?): ByteArray? {
        if (fileUri.isNullOrBlank()) return null
        return try {
            val cleanUri = if (fileUri.contains("?")) fileUri.substringBefore("?") else fileUri
            if (cleanUri.startsWith("content://")) {
                val context = try {
                    com.google.firebase.FirebaseApp.getInstance().applicationContext
                } catch (_: Exception) {
                    null
                }
                if (context != null) {
                    return context.contentResolver.openInputStream(cleanUri.toUri())?.use { it.readBytes() }
                }
            }
            val path = if (cleanUri.startsWith("file://")) {
                cleanUri.removePrefix("file://")
            } else if (cleanUri.startsWith("/")) {
                cleanUri
            } else {
                cleanUri.toUri().path
            }
            if (path != null) {
                val f = java.io.File(path)
                if (f.exists() && f.length() > 0) f.readBytes() else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun ensureAuth() {
        val currentAuth = auth ?: return
        if (currentAuth.currentUser == null) {
            try {
                currentAuth.signInAnonymously().await()
            } catch (_: Exception) {
                // Anonymous auth fallback
            }
        }
    }

    private fun listenToSpaceChanges(spaceId: String) {
        spaceListenerRegistration?.remove()
        val firestore = db ?: return

        spaceListenerRegistration = firestore.collection("spaces").document(spaceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    _currentSpace.value = null
                    return@addSnapshotListener
                }

                val isActive = snapshot.getBoolean("isActive") ?: true
                if (!isActive) {
                    _currentSpace.value = null
                    return@addSnapshotListener
                }

                val partnerA = snapshot.getString("partnerAName")
                    ?: snapshot.getString("partner1Name") ?: "Alex"
                val partnerB = snapshot.getString("partnerBName")
                    ?: snapshot.getString("partner2Name") ?: "Sam"

                val isSetupComplete = snapshot.getBoolean("isSetupComplete")
                    ?: (partnerA != "Alex" || partnerB != "Sam")

                val partner1PhotoUrl = snapshot.getString("partner1PhotoUrl") ?: snapshot.getString("partnerAPhotoUrl")
                val partner2PhotoUrl = snapshot.getString("partner2PhotoUrl") ?: snapshot.getString("partnerBPhotoUrl")
                val partner1ColorHex = snapshot.getString("partner1ColorHex") ?: "#FF6B6B"
                val partner2ColorHex = snapshot.getString("partner2ColorHex") ?: "#4ECDC4"
                val userUids = (snapshot.get("userUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                val updatedSpace = CoupleSpace(
                    id = snapshot.id,
                    partnerAName = partnerA,
                    partnerBName = partnerB,
                    anniversaryYear = snapshot.getLong("anniversaryYear")?.toInt() ?: 2025,
                    anniversaryMonth = snapshot.getLong("anniversaryMonth")?.toInt() ?: 6,
                    anniversaryDay = snapshot.getLong("anniversaryDay")?.toInt() ?: 25,
                    anniversaryEpochMillis = snapshot.getLong("anniversaryEpochMillis") ?: 1750800000000L,
                    isSetupComplete = isSetupComplete,
                    isActive = true,
                    userUids = userUids,
                    partner1Id = snapshot.getString("partner1Id"),
                    partner2Id = snapshot.getString("partner2Id"),
                    partner1PhotoUrl = partner1PhotoUrl,
                    partner2PhotoUrl = partner2PhotoUrl,
                    partner1ColorHex = partner1ColorHex,
                    partner2ColorHex = partner2ColorHex
                )
                _currentSpace.value = updatedSpace
            }
    }

    override fun getMemories(coupleId: String): Flow<List<Memory>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("spaces")
            .document(coupleId)
            .collection("memories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val memoriesList = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val dateStr = doc.getString("date")
                        val date = if (!dateStr.isNullOrEmpty()) {
                            LocalDate.parse(dateStr)
                        } else {
                            LocalDate.now()
                        }
                        val (partnerAUrl, partnerBUrl) = resolveMemorySlots(
                            legacyUrl = sanitizeImageUrl(doc.getString("imageUrl")),
                            partnerAUrl = sanitizeImageUrl(doc.getString("partnerAImageUrl")),
                            partnerBUrl = sanitizeImageUrl(doc.getString("partnerBImageUrl")),
                        )
                        Memory(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            date = date,
                            note = doc.getString("note") ?: "",
                            partnerAImageUrl = partnerAUrl,
                            partnerBImageUrl = partnerBUrl
                        )
                    } catch (_: Exception) {
                        null
                    }
                }?.sortedByDescending { it.date } ?: emptyList()

                trySend(memoriesList)
            }

        awaitClose {
            registration.remove()
        }
    }

    override suspend fun addMemory(
        coupleId: String,
        memory: Memory,
        imageABytes: ByteArray?,
        imageBBytes: ByteArray?
    ): Result<Memory> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore uninitialized"))
        val memoryId = memory.id.ifBlank { UUID.randomUUID().toString() }

        val finalUrlA = persistMemorySlot(coupleId, memoryId, "a", memory.partnerAImageUrl, imageABytes)
        val finalUrlB = persistMemorySlot(coupleId, memoryId, "b", memory.partnerBImageUrl, imageBBytes)

        val memoryData = mapOf(
            "id" to memoryId,
            "title" to memory.title,
            "date" to memory.date.toString(),
            "note" to memory.note,
            "partnerAImageUrl" to finalUrlA,
            "partnerBImageUrl" to finalUrlB,
            "createdAt" to System.currentTimeMillis()
        )

        return try {
            firestore.collection("spaces")
                .document(coupleId)
                .collection("memories")
                .document(memoryId)
                .set(memoryData)
                .await()

            Result.success(
                memory.copy(
                    id = memoryId,
                    imageUrl = null,
                    partnerAImageUrl = finalUrlA,
                    partnerBImageUrl = finalUrlB
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMemory(
        coupleId: String,
        memory: Memory,
        imageABytes: ByteArray?,
        imageBBytes: ByteArray?
    ): Result<Memory> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore uninitialized"))
        val memoryRef = firestore.collection("spaces")
            .document(coupleId)
            .collection("memories")
            .document(memory.id)
        val replacesA = imageABytes?.isNotEmpty() == true
        val replacesB = imageBBytes?.isNotEmpty() == true
        val previous = if (replacesA || replacesB) {
            try {
                withTimeoutOrNull(2.seconds) { memoryRef.get().await() }
            } catch (_: Exception) {
                null
            }
        } else null

        val finalUrlA = persistMemorySlot(coupleId, memory.id, "a", memory.partnerAImageUrl, imageABytes)
        val finalUrlB = persistMemorySlot(coupleId, memory.id, "b", memory.partnerBImageUrl, imageBBytes)

        val memoryData = mapOf(
            "id" to memory.id,
            "title" to memory.title,
            "date" to memory.date.toString(),
            "note" to memory.note,
            "imageUrl" to FieldValue.delete(),
            "partnerAImageUrl" to finalUrlA,
            "partnerBImageUrl" to finalUrlB,
            "updatedAt" to System.currentTimeMillis()
        )

        return try {
            withTimeoutOrNull(10.seconds) {
                memoryRef.set(memoryData, SetOptions.merge()).await()
            }
            val keptUrls = setOf(finalUrlA, finalUrlB)
            if (replacesA) previous?.getString("partnerAImageUrl")?.takeIf { it !in keptUrls }?.let { deleteStorageFile(it) }
            if (replacesB) previous?.getString("partnerBImageUrl")?.takeIf { it !in keptUrls }?.let { deleteStorageFile(it) }
            Result.success(
                memory.copy(
                    imageUrl = null,
                    partnerAImageUrl = finalUrlA,
                    partnerBImageUrl = finalUrlB
                )
            )
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
        val url = sanitizeImageUrl(currentUrl)
        val effectiveBytes = bytes?.takeIf { it.isNotEmpty() } ?: if (isLocalUri(url)) readLocalFileBytes(url) else null
        if (effectiveBytes == null || effectiveBytes.isEmpty()) {
            return if (isLocalUri(url)) null else url
        }

        val remoteUrl = try {
            ensureAuth()
            val storageRef = storage?.reference
                ?.child("couples")
                ?.child(coupleId)
                ?.child("memories")
                ?.child(storageFileName("${memoryId}_$slot"))
            storageRef?.let { ref ->
                withTimeoutOrNull(10.seconds) {
                    ref.putBytes(effectiveBytes).await()
                    ref.downloadUrl.await().toString()
                }
            }
        } catch (_: Exception) {
            null
        }

        return remoteUrl ?: try {
            "data:image/jpeg;base64,${android.util.Base64.encodeToString(effectiveBytes, android.util.Base64.NO_WRAP)}"
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun deleteMemory(coupleId: String, memoryId: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore uninitialized"))
        val memoryRef = firestore.collection("spaces")
            .document(coupleId)
            .collection("memories")
            .document(memoryId)
        return try {
            val snapshot = try {
                withTimeoutOrNull(2.seconds) { memoryRef.get().await() }
            } catch (_: Exception) {
                null
            }
            listOf("partnerAImageUrl", "partnerBImageUrl", "imageUrl")
                .mapNotNull { snapshot?.getString(it) }
                .distinct()
                .forEach { deleteStorageFile(it) }
            // Fixed slot paths used before uploads got unique names
            for (legacyName in listOf("${memoryId}_a.jpg", "${memoryId}_b.jpg")) {
                try {
                    storage?.reference
                        ?.child("couples")
                        ?.child(coupleId)
                        ?.child("memories")
                        ?.child(legacyName)
                        ?.delete()
                        ?.await()
                } catch (_: Exception) {
                    // Ignore if not found
                }
            }

            withTimeoutOrNull(2.seconds) {
                memoryRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePartnerNames(
        coupleId: String,
        partnerAName: String,
        partnerBName: String
    ): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore uninitialized"))
        return try {
            firestore.collection("spaces").document(coupleId).update(
                mapOf(
                    "partnerAName" to partnerAName,
                    "partnerBName" to partnerBName,
                    "partner1Name" to partnerAName,
                    "partner2Name" to partnerBName,
                    "isSetupComplete" to true
                )
            ).await()
            _currentSpace.value = _currentSpace.value?.copy(
                partnerAName = partnerAName,
                partnerBName = partnerBName,
                isSetupComplete = true
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSpaceDetails(
        coupleId: String,
        partnerAName: String,
        partnerBName: String,
        anniversaryYear: Int,
        anniversaryMonth: Int,
        anniversaryDay: Int
    ): Result<Unit> {
        val firestore = db
        val localDate = LocalDate.of(anniversaryYear, anniversaryMonth, anniversaryDay)
        val epochMillis = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (firestore != null) {
            try {
                val updateData = mutableMapOf<String, Any>(
                    "partnerAName" to partnerAName,
                    "partnerBName" to partnerBName,
                    "partner1Name" to partnerAName,
                    "partner2Name" to partnerBName,
                    "anniversaryYear" to anniversaryYear,
                    "anniversaryMonth" to anniversaryMonth,
                    "anniversaryDay" to anniversaryDay,
                    "anniversaryEpochMillis" to epochMillis,
                    "isSetupComplete" to true,
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("spaces").document(coupleId)
                    .set(updateData, SetOptions.merge())
                    .await()

                if (auth?.currentUser != null) {
                    linkCurrentUserToSpace(coupleId)
                }
            } catch (_: Exception) {
                // Ignore network error and update locally
            }
        }

        _currentSpace.value = _currentSpace.value?.copy(
            partnerAName = partnerAName,
            partnerBName = partnerBName,
            anniversaryYear = anniversaryYear,
            anniversaryMonth = anniversaryMonth,
            anniversaryDay = anniversaryDay,
            anniversaryEpochMillis = epochMillis,
            isSetupComplete = true
        )
        return Result.success(Unit)
    }

    override suspend fun listenToPairingCode(code: String) {
        val cleanCode = code.replace("-", "").trim().uppercase()
        if (cleanCode.length != 6) return
        try {
            val firestore = db
            if (firestore != null) {
                val spaceDocRef = firestore.collection("spaces").document("space_$cleanCode")
                val snapshot = withTimeoutOrNull(2.seconds) { spaceDocRef.get().await() }
                if (snapshot == null || !snapshot.exists()) {
                    val spaceData = mapOf(
                        "id" to "space_$cleanCode",
                        "pairingCode" to cleanCode,
                        "partnerAName" to "Alex",
                        "partnerBName" to "Sam",
                        "anniversaryYear" to 2025,
                        "anniversaryMonth" to 6,
                        "anniversaryDay" to 25,
                        "isSetupComplete" to false,
                        "isActive" to false,
                        "createdAt" to System.currentTimeMillis()
                    )
                    withTimeoutOrNull(2.seconds) { spaceDocRef.set(spaceData).await() }
                }
                listenToSpaceChanges("space_$cleanCode")
            }
        } catch (_: Exception) {
            // Ignore if Firebase uninitialized
        }
    }

    override suspend fun generateNewPairingCode(): PairingCode {
        val digits = (100000..999999).random().toString()
        val generatedCode = "${digits.take(3)}-${digits.drop(3)}"

        listenToPairingCode(generatedCode)

        return PairingCode(code = generatedCode, totalValidSeconds = 900)
    }

    override suspend fun connectWithCode(code: String): Result<CoupleSpace> {
        val cleanCode = code.replace("-", "").trim().uppercase()

        if (cleanCode.length != 6 || !cleanCode.all { it in '0'..'9' }) {
            return Result.failure(
                IllegalArgumentException("Der Code muss aus genau 6 Ziffern bestehen (z. B. 482-913).")
            )
        }

        try {
            val firestore = db
            if (firestore != null) {
                val spaceDocRef = firestore.collection("spaces").document("space_$cleanCode")
                var snapshot = withTimeoutOrNull(2.seconds) { spaceDocRef.get().await() }

                if (snapshot == null || !snapshot.exists()) {
                    val query = withTimeoutOrNull(2.seconds) {
                        firestore.collection("spaces")
                            .whereEqualTo("pairingCode", cleanCode)
                            .get()
                            .await()
                    }
                    if (query != null && !query.isEmpty) {
                        snapshot = query.documents.first()
                    }
                }

                if (snapshot != null && snapshot.exists()) {
                    val members = (snapshot.get("userUids") as? List<*>)
                        ?: (snapshot.get("members") as? List<*>)
                        ?: emptyList<Any>()
                    val partner2Id = snapshot.getString("partner2Id")
                    val currentUid = auth?.currentUser?.uid

                    val isAlreadyMember = currentUid != null && (
                        currentUid in members ||
                        currentUid == snapshot.getString("partner1Id") ||
                        currentUid == partner2Id
                    )
                    val isRoomFull = members.size >= 2 || (!partner2Id.isNullOrBlank() && partner2Id != currentUid)

                    if (isRoomFull && !isAlreadyMember) {
                        return Result.failure(
                            IllegalStateException("Dieser Beziehungsraum ist bereits voll (maximal 2 Partner).")
                        )
                    }
                }

                val docId = if (snapshot != null && snapshot.exists()) snapshot.id else "space_$cleanCode"
                val partnerA = snapshot?.getString("partnerAName") ?: snapshot?.getString("partner1Name") ?: "Alex"
                val partnerB = snapshot?.getString("partnerBName") ?: snapshot?.getString("partner2Name") ?: "Sam"
                val isSetupComplete = snapshot?.getBoolean("isSetupComplete") ?: false
                val partner1ColorHex = snapshot?.getString("partner1ColorHex") ?: "#FF6B6B"
                val partner2ColorHex = snapshot?.getString("partner2ColorHex") ?: "#4ECDC4"

                withTimeoutOrNull(2.seconds) {
                    firestore.collection("spaces").document(docId).set(
                        mapOf(
                            "id" to docId,
                            "pairingCode" to cleanCode,
                            "partnerAName" to partnerA,
                            "partnerBName" to partnerB,
                            "partner1Name" to partnerA,
                            "partner2Name" to partnerB,
                            "anniversaryYear" to (snapshot?.getLong("anniversaryYear")?.toInt() ?: 2025),
                            "anniversaryMonth" to (snapshot?.getLong("anniversaryMonth")?.toInt() ?: 6),
                            "anniversaryDay" to (snapshot?.getLong("anniversaryDay")?.toInt() ?: 25),
                            "isSetupComplete" to isSetupComplete,
                            "isActive" to true,
                            "pairedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    ).await()
                }

                val membersList = (snapshot?.get("userUids") as? List<*>)
                    ?: (snapshot?.get("members") as? List<*>)
                    ?: emptyList<Any>()
                val userUids = membersList.filterIsInstance<String>()

                val pairedSpace = CoupleSpace(
                    id = docId,
                    partnerAName = partnerA,
                    partnerBName = partnerB,
                    anniversaryYear = snapshot?.getLong("anniversaryYear")?.toInt() ?: 2025,
                    anniversaryMonth = snapshot?.getLong("anniversaryMonth")?.toInt() ?: 6,
                    anniversaryDay = snapshot?.getLong("anniversaryDay")?.toInt() ?: 25,
                    isSetupComplete = isSetupComplete,
                    isActive = true,
                    userUids = userUids,
                    partner1Id = snapshot?.getString("partner1Id"),
                    partner2Id = snapshot?.getString("partner2Id"),
                    partner1PhotoUrl = snapshot?.getString("partner1PhotoUrl"),
                    partner2PhotoUrl = snapshot?.getString("partner2PhotoUrl"),
                    partner1ColorHex = partner1ColorHex,
                    partner2ColorHex = partner2ColorHex
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

        val pairedSpace = CoupleSpace(
            id = "space_$cleanCode",
            partnerAName = "Alex",
            partnerBName = "Sam",
            anniversaryYear = 2025,
            anniversaryMonth = 6,
            anniversaryDay = 25,
            isSetupComplete = false,
            isActive = true
        )
        _currentSpace.value = pairedSpace
        listenToSpaceChanges(pairedSpace.id)
        return Result.success(pairedSpace)
    }

    override suspend fun restoreSession(coupleId: String): Result<CoupleSpace> {
        try {
            val firestore = db
            if (firestore != null) {
                val doc = withTimeoutOrNull(2.seconds) {
                    firestore.collection("spaces").document(coupleId).get().await()
                }
                if (doc != null && doc.exists()) {
                    val isActive = doc.getBoolean("isActive") ?: true
                    if (!isActive) {
                        _currentSpace.value = null
                        return Result.failure(IllegalStateException("Couple space is inactive"))
                    }
                    val partnerA = doc.getString("partnerAName") ?: doc.getString("partner1Name") ?: "Alex"
                    val partnerB = doc.getString("partnerBName") ?: doc.getString("partner2Name") ?: "Sam"
                    val partner1PhotoUrl = doc.getString("partner1PhotoUrl") ?: doc.getString("partnerAPhotoUrl")
                    val partner2PhotoUrl = doc.getString("partner2PhotoUrl") ?: doc.getString("partnerBPhotoUrl")
                    val partner1ColorHex = doc.getString("partner1ColorHex") ?: "#FF6B6B"
                    val partner2ColorHex = doc.getString("partner2ColorHex") ?: "#4ECDC4"
                    val userUids = (doc.get("userUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val space = CoupleSpace(
                        id = doc.id,
                        partnerAName = partnerA,
                        partnerBName = partnerB,
                        anniversaryYear = doc.getLong("anniversaryYear")?.toInt() ?: 2025,
                        anniversaryMonth = doc.getLong("anniversaryMonth")?.toInt() ?: 6,
                        anniversaryDay = doc.getLong("anniversaryDay")?.toInt() ?: 25,
                        isSetupComplete = doc.getBoolean("isSetupComplete") ?: true,
                        isActive = true,
                        userUids = userUids,
                        partner1Id = doc.getString("partner1Id"),
                        partner2Id = doc.getString("partner2Id"),
                        partner1PhotoUrl = partner1PhotoUrl,
                        partner2PhotoUrl = partner2PhotoUrl,
                        partner1ColorHex = partner1ColorHex,
                        partner2ColorHex = partner2ColorHex
                    )
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
            }
        } catch (_: Exception) {
            // Fallback if offline
        }

        val space = CoupleSpace(
            id = coupleId,
            partnerAName = "Alex",
            partnerBName = "Sam",
            anniversaryYear = 2025,
            anniversaryMonth = 6,
            anniversaryDay = 25,
            isSetupComplete = true,
            isActive = true
        )
        _currentSpace.value = space
        listenToSpaceChanges(space.id)
        return Result.success(space)
    }

    override suspend fun restoreSessionForUser(uid: String): Result<CoupleSpace?> {
        val firestore = db ?: return Result.success(null)
        try {
            val userDoc = withTimeoutOrNull(2.seconds) {
                firestore.collection("users").document(uid).get().await()
            }
            if (userDoc != null && userDoc.exists()) {
                val coupleId = userDoc.getString("coupleId")
                _currentUserProfile.value = UserProfile(
                    uid = uid,
                    email = userDoc.getString("email"),
                    displayName = userDoc.getString("displayName"),
                    coupleId = coupleId
                )

                if (!coupleId.isNullOrBlank()) {
                    val spaceRes = restoreSession(coupleId)
                    return Result.success(spaceRes.getOrNull())
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
            db?.collection("spaces")?.document(coupleId)?.update(
                mapOf(
                    "isActive" to false,
                    "disconnectedAt" to System.currentTimeMillis()
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
            partnerAName = "Alex",
            partnerBName = "Sam",
            anniversaryYear = 2025,
            anniversaryMonth = 6,
            anniversaryDay = 25,
            isSetupComplete = true,
            isActive = true
        )

        try {
            withTimeoutOrNull(2.seconds) {
                db?.collection("spaces")?.document(DEMO_SPACE_ID)?.set(
                    mapOf(
                        "id" to demoSpace.id,
                        "partnerAName" to demoSpace.partnerAName,
                        "partnerBName" to demoSpace.partnerBName,
                        "partner1Name" to demoSpace.partnerAName,
                        "partner2Name" to demoSpace.partnerBName,
                        "anniversaryYear" to demoSpace.anniversaryYear,
                        "anniversaryMonth" to demoSpace.anniversaryMonth,
                        "anniversaryDay" to demoSpace.anniversaryDay,
                        "isSetupComplete" to true,
                        "isActive" to true
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
                val userDocRef = firestore.collection("users").document(uid)
                val snapshot = withTimeoutOrNull(2.seconds) { userDocRef.get().await() }

                if (snapshot != null && snapshot.exists()) {
                    val existingCoupleId = snapshot.getString("coupleId")
                    if (!existingCoupleId.isNullOrBlank()) {
                        coupleId = existingCoupleId
                        restoreSession(existingCoupleId)
                    }
                }

                val userData = mutableMapOf<String, Any?>(
                    "uid" to uid,
                    "email" to email,
                    "displayName" to displayName,
                    "updatedAt" to System.currentTimeMillis()
                )
                if (!coupleId.isNullOrBlank()) {
                    userData["coupleId"] = coupleId
                }

                userDocRef.set(userData, SetOptions.merge()).await()

                if (!coupleId.isNullOrBlank() && coupleId != DEMO_SPACE_ID) {
                    claimPartnerSlot(coupleId, uid, preferPartner2 = false, replaceUid = previousUid)
                }
            } catch (_: Exception) {
                // Ignore network error and keep local state
            }
        }

        val profile = UserProfile(
            uid = uid,
            email = email,
            displayName = displayName,
            coupleId = coupleId
        )
        _currentUserProfile.value = profile
        return Result.success(profile)
    }

    override suspend fun linkCurrentUserToSpace(coupleId: String): Result<Unit> {
        val user = auth?.currentUser ?: return Result.success(Unit)
        val firestore = db ?: return Result.success(Unit)

        try {
            firestore.collection("users").document(user.uid).set(
                mapOf(
                    "uid" to user.uid,
                    "email" to user.email,
                    "displayName" to user.displayName,
                    "coupleId" to coupleId,
                    "updatedAt" to System.currentTimeMillis()
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

    override suspend fun uploadProfilePhoto(
        coupleId: String,
        isPartner1: Boolean,
        imageBytes: ByteArray
    ): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val previousSpace = _currentSpace.value
        val previousUrl = if (isPartner1) previousSpace?.partner1PhotoUrl else previousSpace?.partner2PhotoUrl
        val partnerUrl = if (isPartner1) previousSpace?.partner2PhotoUrl else previousSpace?.partner1PhotoUrl
        val context = try {
            com.google.firebase.FirebaseApp.getInstance().applicationContext
        } catch (_: Exception) {
            null
        }

        // 1. Immediately save locally for 0ms UI latency and offline resilience
        val localUrl = if (context != null) {
            com.aistudio.couplebubble.qxztrw.data.LocalImageStorage.saveProfilePhoto(context, coupleId, isPartner1, imageBytes)
        } else null

        if (localUrl != null) {
            _currentSpace.value = if (isPartner1) {
                _currentSpace.value?.copy(partner1PhotoUrl = localUrl)
            } else {
                _currentSpace.value?.copy(partner2PhotoUrl = localUrl)
            }
        }

        // 2. Prepare Base64 Data-URI for resilient cross-device sync
        val base64DataUri = try {
            val base64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (_: Exception) {
            null
        }

        // 3. Upload to Firebase Storage with strict 10s timeout. The path is unique per upload: a fixed
        // per-slot path would, after a role swap, overwrite the file the partner's URL still points to.
        var downloadUrl: String? = null
        try {
            ensureAuth()
            val storageRef = storage?.reference
                ?.child("couples")
                ?.child(coupleId)
                ?.child("profiles")
                ?.child(storageFileName("profile"))

            if (storageRef != null) {
                withTimeoutOrNull(10.seconds) {
                    storageRef.putBytes(imageBytes).await()
                    downloadUrl = storageRef.downloadUrl.await().toString()
                }
            }
        } catch (_: Exception) {
            // Storage upload failed or offline; Base64 fallback will be used
        }

        // 4. Prefer remote Cloud Storage URL; fallback to Base64 data URI for Firestore sync
        val syncUrl = downloadUrl ?: base64DataUri

        // 5. Always update Firestore so partner devices receive the photo immediately
        if (syncUrl != null) {
            val firestore = db
            if (firestore != null) {
                try {
                    val fieldName = if (isPartner1) "partner1PhotoUrl" else "partner2PhotoUrl"
                    val aliasFieldName = if (isPartner1) "partnerAPhotoUrl" else "partnerBPhotoUrl"

                    withTimeoutOrNull(5.seconds) {
                        firestore.collection("spaces").document(coupleId).set(
                            mapOf(
                                fieldName to syncUrl,
                                aliasFieldName to syncUrl,
                                "updatedAt" to System.currentTimeMillis()
                            ),
                            SetOptions.merge()
                        ).await()
                    }

                    _currentSpace.value = if (isPartner1) {
                        _currentSpace.value?.copy(partner1PhotoUrl = syncUrl)
                    } else {
                        _currentSpace.value?.copy(partner2PhotoUrl = syncUrl)
                    }
                    if (previousUrl != syncUrl && previousUrl != partnerUrl) {
                        deleteStorageFile(previousUrl)
                    }
                } catch (_: Exception) {
                    // Firestore sync delayed; local state preserved
                }
            }
        }

        val finalUrl = syncUrl ?: localUrl
        if (!finalUrl.isNullOrBlank()) {
            Result.success(finalUrl)
        } else {
            Result.failure(IllegalStateException("Konnte Profilbild nicht speichern"))
        }
    }

    override suspend fun uploadProfilePhoto(
        coupleId: String,
        isPartner1: Boolean,
        imageUri: android.net.Uri
    ): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val context = try {
            com.google.firebase.FirebaseApp.getInstance().applicationContext
        } catch (_: Exception) {
            null
        }

        val bytes = if (context != null) {
            com.aistudio.couplebubble.qxztrw.data.LocalImageStorage.compressImage(context, imageUri)
        } else null

        if (bytes != null && bytes.isNotEmpty()) {
            uploadProfilePhoto(coupleId, isPartner1, bytes)
        } else {
            Result.failure(IllegalStateException("Bild konnte nicht gelesen werden"))
        }
    }

    override suspend fun updatePartnerColor(
        coupleId: String,
        isPartner1: Boolean,
        colorHex: String
    ): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore uninitialized"))
        val fieldName = if (isPartner1) "partner1ColorHex" else "partner2ColorHex"
        val aliasFieldName = if (isPartner1) "partnerAColorHex" else "partnerBColorHex"

        return try {
            firestore.collection("spaces").document(coupleId).set(
                mapOf(
                    fieldName to colorHex,
                    aliasFieldName to colorHex,
                    "updatedAt" to System.currentTimeMillis()
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
        val spaceRef = firestore.collection("spaces").document(coupleId)
        try {
            withTimeoutOrNull(5.seconds) {
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(spaceRef)
                    if (!snapshot.exists()) return@runTransaction
                    val storedUids = (snapshot.get("userUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    var p1 = snapshot.getString("partner1Id")
                    var p2 = snapshot.getString("partner2Id")
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
                    if (p1 != snapshot.getString("partner1Id") ||
                        p2 != snapshot.getString("partner2Id") ||
                        newUids != storedUids
                    ) {
                        transaction.update(
                            spaceRef,
                            mapOf(
                                "partner1Id" to (p1 ?: FieldValue.delete()),
                                "partner2Id" to (p2 ?: FieldValue.delete()),
                                "userUids" to newUids
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
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore uninitialized"))

        return try {
            val spaceRef = firestore.collection("spaces").document(coupleId)
            val memoryRefs = spaceRef.collection("memories").get().await().documents.map { it.reference }
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

                val userUids = (snapshot.get("userUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val hasExplicitIds = snapshot.getString("partner1Id") != null || snapshot.getString("partner2Id") != null
                val p1Id = if (hasExplicitIds) snapshot.getString("partner1Id") else userUids.getOrNull(0)
                val p2Id = if (hasExplicitIds) snapshot.getString("partner2Id") else userUids.getOrNull(1)
                // With only one known member the swap would move their data to slot 2 while they stay partner 1
                if ((p1Id == null) != (p2Id == null)) {
                    throw PartnerNotConnectedException()
                }

                val p1Name = snapshot.getString("partnerAName") ?: snapshot.getString("partner1Name") ?: "Alex"
                val p2Name = snapshot.getString("partnerBName") ?: snapshot.getString("partner2Name") ?: "Sam"
                val p1Photo = snapshot.getString("partner1PhotoUrl") ?: snapshot.getString("partnerAPhotoUrl")
                val p2Photo = snapshot.getString("partner2PhotoUrl") ?: snapshot.getString("partnerBPhotoUrl")
                val p1Color = snapshot.getString("partner1ColorHex") ?: "#FF6B6B"
                val p2Color = snapshot.getString("partner2ColorHex") ?: "#4ECDC4"

                // Missing values must be deleted, not skipped, or the old photo would stay in both slots
                val newP1Photo: Any = p2Photo ?: FieldValue.delete()
                val newP2Photo: Any = p1Photo ?: FieldValue.delete()
                val spaceUpdate = mutableMapOf<String, Any>(
                    "partnerAName" to p2Name,
                    "partnerBName" to p1Name,
                    "partner1Name" to p2Name,
                    "partner2Name" to p1Name,
                    "partner1PhotoUrl" to newP1Photo,
                    "partner2PhotoUrl" to newP2Photo,
                    "partnerAPhotoUrl" to newP1Photo,
                    "partnerBPhotoUrl" to newP2Photo,
                    "partner1ColorHex" to p2Color,
                    "partner2ColorHex" to p1Color,
                    "partnerAColorHex" to p2Color,
                    "partnerBColorHex" to p1Color,
                    "updatedAt" to System.currentTimeMillis()
                )
                if (p1Id != null && p2Id != null) {
                    spaceUpdate["partner1Id"] = p2Id
                    spaceUpdate["partner2Id"] = p1Id
                    spaceUpdate["userUids"] = listOf(p2Id, p1Id)
                }
                transaction.update(spaceRef, spaceUpdate)

                for (memory in memorySnapshots) {
                    if (!memory.exists()) continue
                    val (slotA, slotB) = resolveMemorySlots(
                        legacyUrl = memory.getString("imageUrl"),
                        partnerAUrl = memory.getString("partnerAImageUrl"),
                        partnerBUrl = memory.getString("partnerBImageUrl"),
                    )
                    transaction.update(
                        memory.reference,
                        mapOf(
                            "partnerAImageUrl" to (slotB ?: FieldValue.delete()),
                            "partnerBImageUrl" to (slotA ?: FieldValue.delete()),
                            "imageUrl" to FieldValue.delete()
                        )
                    )
                }
                listOfNotNull(p2Id, p1Id)
            }.await()

            _currentSpace.value?.let { current ->
                _currentSpace.value = current.copy(
                    partnerAName = current.partnerBName,
                    partnerBName = current.partnerAName,
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
