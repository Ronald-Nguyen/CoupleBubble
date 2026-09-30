package com.aistudio.couplebubble.qxztrw.repository

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

class FirebaseCoupleRepository : CoupleRepository {

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
            } catch (e: Exception) {
                // Settings can only be set before any other Firestore operations
            }
            firestore
        } catch (e: Exception) {
            null
        }
    }

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val storage: FirebaseStorage? by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            try {
                FirebaseStorage.getInstance("gs://couplebubble-a8139.firebasestorage.app")
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun sanitizeImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true) ||
            trimmed.startsWith("content://", ignoreCase = true) ||
            trimmed.startsWith("data:image/", ignoreCase = true) -> trimmed
            trimmed.startsWith("/") -> "file://$trimmed"
            else -> null
        }
    }

    private suspend fun ensureAuth() {
        val currentAuth = auth ?: return
        if (currentAuth.currentUser == null) {
            try {
                currentAuth.signInAnonymously().await()
            } catch (e: Exception) {
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
                    isActive = isActive,
                    userUids = userUids,
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
                        val legacyUrl = sanitizeImageUrl(doc.getString("imageUrl"))
                        val partnerAUrl = sanitizeImageUrl(doc.getString("partnerAImageUrl")) ?: legacyUrl
                        val partnerBUrl = sanitizeImageUrl(doc.getString("partnerBImageUrl"))
                        Memory(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            date = date,
                            note = doc.getString("note") ?: "",
                            imageUrl = partnerAUrl ?: partnerBUrl,
                            partnerAImageUrl = partnerAUrl,
                            partnerBImageUrl = partnerBUrl
                        )
                    } catch (e: Exception) {
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
        val memoryId = if (memory.id.isNotBlank()) memory.id else UUID.randomUUID().toString()

        var urlA = sanitizeImageUrl(memory.partnerAImageUrl ?: memory.imageUrl)
        var urlB = sanitizeImageUrl(memory.partnerBImageUrl)

        if (imageABytes != null && imageABytes.isNotEmpty()) {
            try {
                ensureAuth()
                val storageRef = storage?.reference
                    ?.child("couples")
                    ?.child(coupleId)
                    ?.child("memories")
                    ?.child("${memoryId}_a.jpg")

                if (storageRef != null) {
                    val remoteUrl = withTimeoutOrNull(10000L) {
                        storageRef.putBytes(imageABytes).await()
                        storageRef.downloadUrl.await().toString()
                    }
                    if (remoteUrl != null) {
                        urlA = remoteUrl
                    }
                }
            } catch (e: Exception) {
                // Proceed with local or fallback image on error
            }
            if (urlA == null || urlA.startsWith("file://")) {
                try {
                    val base64 = android.util.Base64.encodeToString(imageABytes, android.util.Base64.NO_WRAP)
                    urlA = "data:image/jpeg;base64,$base64"
                } catch (e: Exception) {
                    // Fallback to local
                }
            }
        }

        if (imageBBytes != null && imageBBytes.isNotEmpty()) {
            try {
                ensureAuth()
                val storageRef = storage?.reference
                    ?.child("couples")
                    ?.child(coupleId)
                    ?.child("memories")
                    ?.child("${memoryId}_b.jpg")

                if (storageRef != null) {
                    val remoteUrl = withTimeoutOrNull(10000L) {
                        storageRef.putBytes(imageBBytes).await()
                        storageRef.downloadUrl.await().toString()
                    }
                    if (remoteUrl != null) {
                        urlB = remoteUrl
                    }
                }
            } catch (e: Exception) {
                // Proceed with local or fallback image on error
            }
            if (urlB == null || urlB.startsWith("file://")) {
                try {
                    val base64 = android.util.Base64.encodeToString(imageBBytes, android.util.Base64.NO_WRAP)
                    urlB = "data:image/jpeg;base64,$base64"
                } catch (e: Exception) {
                    // Fallback to local
                }
            }
        }

        val finalUrlA = sanitizeImageUrl(urlA)
        val finalUrlB = sanitizeImageUrl(urlB)

        val memoryData = mapOf(
            "id" to memoryId,
            "title" to memory.title,
            "date" to memory.date.toString(),
            "note" to memory.note,
            "imageUrl" to (finalUrlA ?: finalUrlB),
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
                    imageUrl = finalUrlA ?: finalUrlB,
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
        var urlA = sanitizeImageUrl(memory.partnerAImageUrl ?: memory.imageUrl)
        var urlB = sanitizeImageUrl(memory.partnerBImageUrl)

        if (imageABytes != null && imageABytes.isNotEmpty()) {
            try {
                ensureAuth()
                val storageRef = storage?.reference
                    ?.child("couples")
                    ?.child(coupleId)
                    ?.child("memories")
                    ?.child("${memory.id}_a.jpg")

                if (storageRef != null) {
                    val remoteUrl = withTimeoutOrNull(10000L) {
                        storageRef.putBytes(imageABytes).await()
                        storageRef.downloadUrl.await().toString()
                    }
                    if (remoteUrl != null) {
                        urlA = remoteUrl
                    }
                }
            } catch (e: Exception) {
                // Ignore storage upload exception or preserve existing
            }
            if (urlA == null || urlA.startsWith("file://")) {
                try {
                    val base64 = android.util.Base64.encodeToString(imageABytes, android.util.Base64.NO_WRAP)
                    urlA = "data:image/jpeg;base64,$base64"
                } catch (e: Exception) {
                    // Fallback to local
                }
            }
        }

        if (imageBBytes != null && imageBBytes.isNotEmpty()) {
            try {
                ensureAuth()
                val storageRef = storage?.reference
                    ?.child("couples")
                    ?.child(coupleId)
                    ?.child("memories")
                    ?.child("${memory.id}_b.jpg")

                if (storageRef != null) {
                    val remoteUrl = withTimeoutOrNull(10000L) {
                        storageRef.putBytes(imageBBytes).await()
                        storageRef.downloadUrl.await().toString()
                    }
                    if (remoteUrl != null) {
                        urlB = remoteUrl
                    }
                }
            } catch (e: Exception) {
                // Ignore storage upload exception or preserve existing
            }
            if (urlB == null || urlB.startsWith("file://")) {
                try {
                    val base64 = android.util.Base64.encodeToString(imageBBytes, android.util.Base64.NO_WRAP)
                    urlB = "data:image/jpeg;base64,$base64"
                } catch (e: Exception) {
                    // Fallback to local
                }
            }
        }

        val finalUrlA = sanitizeImageUrl(urlA)
        val finalUrlB = sanitizeImageUrl(urlB)

        val memoryData = mapOf(
            "id" to memory.id,
            "title" to memory.title,
            "date" to memory.date.toString(),
            "note" to memory.note,
            "imageUrl" to (finalUrlA ?: finalUrlB),
            "partnerAImageUrl" to finalUrlA,
            "partnerBImageUrl" to finalUrlB,
            "updatedAt" to System.currentTimeMillis()
        )

        return try {
            withTimeoutOrNull(10000L) {
                firestore.collection("spaces")
                    .document(coupleId)
                    .collection("memories")
                    .document(memory.id)
                    .set(memoryData, SetOptions.merge())
                    .await()
            }
            Result.success(
                memory.copy(
                    imageUrl = finalUrlA ?: finalUrlB,
                    partnerAImageUrl = finalUrlA,
                    partnerBImageUrl = finalUrlB
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMemory(coupleId: String, memoryId: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore uninitialized"))
        return try {
            try {
                storage?.reference
                    ?.child("couples")
                    ?.child(coupleId)
                    ?.child("memories")
                    ?.child("${memoryId}_a.jpg")
                    ?.delete()
                    ?.await()
            } catch (e: Exception) {
                // Ignore if not found
            }
            try {
                storage?.reference
                    ?.child("couples")
                    ?.child(coupleId)
                    ?.child("memories")
                    ?.child("${memoryId}_b.jpg")
                    ?.delete()
                    ?.await()
            } catch (e: Exception) {
                // Ignore if not found
            }

            withTimeoutOrNull(2000L) {
                firestore.collection("spaces")
                    .document(coupleId)
                    .collection("memories")
                    .document(memoryId)
                    .delete()
                    .await()
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

                val currentUser = auth?.currentUser
                if (currentUser != null) {
                    updateData["userUids"] = FieldValue.arrayUnion(currentUser.uid)
                }

                firestore.collection("spaces").document(coupleId)
                    .set(updateData, SetOptions.merge())
                    .await()

                if (currentUser != null) {
                    linkCurrentUserToSpace(coupleId)
                }
            } catch (e: Exception) {
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
                val snapshot = withTimeoutOrNull(2000L) { spaceDocRef.get().await() }
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
                    withTimeoutOrNull(2000L) { spaceDocRef.set(spaceData).await() }
                }
                listenToSpaceChanges("space_$cleanCode")
            }
        } catch (e: Exception) {
            // Ignore if Firebase uninitialized
        }
    }

    override suspend fun generateNewPairingCode(): PairingCode {
        val prefixes = listOf("BLU", "LUV", "JOY", "SUN", "DUO")
        val number = (100..999).random()
        val generatedCode = "${prefixes.random()}-$number"

        listenToPairingCode(generatedCode)

        return PairingCode(code = generatedCode, totalValidSeconds = 900)
    }

    override suspend fun connectWithCode(code: String): Result<CoupleSpace> {
        val cleanCode = code.replace("-", "").trim().uppercase()

        if (cleanCode.length != 6) {
            return Result.failure(
                IllegalArgumentException("Der Code muss genau 6 Zeichen lang sein (z. B. BLU-789).")
            )
        }

        try {
            val firestore = db
            if (firestore != null) {
                val spaceDocRef = firestore.collection("spaces").document("space_$cleanCode")
                var snapshot = withTimeoutOrNull(2000L) { spaceDocRef.get().await() }

                if (snapshot == null || !snapshot.exists()) {
                    val query = withTimeoutOrNull(2000L) {
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

                withTimeoutOrNull(2000L) {
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
                    partner1PhotoUrl = snapshot?.getString("partner1PhotoUrl"),
                    partner2PhotoUrl = snapshot?.getString("partner2PhotoUrl"),
                    partner1ColorHex = partner1ColorHex,
                    partner2ColorHex = partner2ColorHex
                )
                _currentSpace.value = pairedSpace
                listenToSpaceChanges(pairedSpace.id)

                val currentUser = auth?.currentUser
                if (currentUser != null) {
                    linkCurrentUserToSpace(pairedSpace.id)
                }

                return Result.success(pairedSpace)
            }
        } catch (e: IllegalStateException) {
            return Result.failure(e)
        } catch (e: Exception) {
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
                val doc = withTimeoutOrNull(2000L) {
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
                        isActive = isActive,
                        userUids = userUids,
                        partner1PhotoUrl = partner1PhotoUrl,
                        partner2PhotoUrl = partner2PhotoUrl,
                        partner1ColorHex = partner1ColorHex,
                        partner2ColorHex = partner2ColorHex
                    )
                    _currentSpace.value = space
                    listenToSpaceChanges(space.id)
                    return Result.success(space)
                }
            }
        } catch (e: Exception) {
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
            val userDoc = withTimeoutOrNull(2000L) {
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
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            // Ignore if Firebase uninitialized
        }
        disconnect()
        return Result.success(Unit)
    }

    override suspend fun openDemoSpace(): CoupleSpace {
        val demoSpace = CoupleSpace(
            id = "demo_space",
            partnerAName = "Alex",
            partnerBName = "Sam",
            anniversaryYear = 2025,
            anniversaryMonth = 6,
            anniversaryDay = 25,
            isSetupComplete = true,
            isActive = true
        )

        try {
            withTimeoutOrNull(2000L) {
                db?.collection("spaces")?.document("demo_space")?.set(
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
        } catch (e: Exception) {
            // Ignore if Firebase uninitialized
        }

        _currentSpace.value = demoSpace
        listenToSpaceChanges(demoSpace.id)
        return demoSpace
    }

    override suspend fun signInWithGoogleUser(
        uid: String,
        email: String?,
        displayName: String?
    ): Result<UserProfile> {
        val firestore = db
        var coupleId: String? = _currentSpace.value?.id

        if (firestore != null) {
            try {
                val userDocRef = firestore.collection("users").document(uid)
                val snapshot = withTimeoutOrNull(2000L) { userDocRef.get().await() }

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

                if (!coupleId.isNullOrBlank()) {
                    firestore.collection("spaces").document(coupleId)
                        .set(mapOf("userUids" to FieldValue.arrayUnion(uid)), SetOptions.merge())
                        .await()
                }
            } catch (e: Exception) {
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

            firestore.collection("spaces").document(coupleId).set(
                mapOf("userUids" to FieldValue.arrayUnion(user.uid)),
                SetOptions.merge()
            ).await()

            _currentUserProfile.value = UserProfile(
                uid = user.uid,
                email = user.email,
                displayName = user.displayName,
                coupleId = coupleId
            )
        } catch (e: Exception) {
            // Ignore error if network fails
        }
        return Result.success(Unit)
    }

    override suspend fun uploadProfilePhoto(
        coupleId: String,
        isPartner1: Boolean,
        imageBytes: ByteArray
    ): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val partnerId = if (isPartner1) "partner1" else "partner2"
        val context = try {
            com.google.firebase.FirebaseApp.getInstance().applicationContext
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            null
        }

        // 3. Upload to Firebase Storage with strict 10s timeout
        var downloadUrl: String? = null
        try {
            ensureAuth()
            val storageRef = storage?.reference
                ?.child("couples")
                ?.child(coupleId)
                ?.child("profiles")
                ?.child("${partnerId}.jpg")

            if (storageRef != null) {
                withTimeoutOrNull(10000L) {
                    storageRef.putBytes(imageBytes).await()
                    downloadUrl = storageRef.downloadUrl.await().toString()
                }
            }
        } catch (e: Exception) {
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

                    withTimeoutOrNull(5000L) {
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
                } catch (e: Exception) {
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
        } catch (e: Exception) {
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

    override suspend fun swapPartners(coupleId: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore uninitialized"))

        return try {
            val spaceRef = firestore.collection("spaces").document(coupleId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(spaceRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("Space document does not exist")
                }

                val p1Id = snapshot.getString("partner1Id") ?: snapshot.getString("partnerAId")
                val p2Id = snapshot.getString("partner2Id") ?: snapshot.getString("partnerBId")
                val p1Name = snapshot.getString("partner1Name") ?: snapshot.getString("partnerAName") ?: "Alex"
                val p2Name = snapshot.getString("partner2Name") ?: snapshot.getString("partnerBName") ?: "Sam"
                val p1Photo = snapshot.getString("partner1PhotoUrl") ?: snapshot.getString("partnerAPhotoUrl")
                val p2Photo = snapshot.getString("partner2PhotoUrl") ?: snapshot.getString("partnerBPhotoUrl")
                val p1Color = snapshot.getString("partner1ColorHex") ?: "#FF6B6B"
                val p2Color = snapshot.getString("partner2ColorHex") ?: "#4ECDC4"
                val userUids = (snapshot.get("userUids") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                val swappedUids = if (userUids.size >= 2) {
                    listOf(userUids[1], userUids[0]) + userUids.drop(2)
                } else userUids

                val updateMap = mutableMapOf<String, Any?>(
                    "partnerAName" to p2Name,
                    "partnerBName" to p1Name,
                    "partner1Name" to p2Name,
                    "partner2Name" to p1Name,
                    "partner1PhotoUrl" to p2Photo,
                    "partner2PhotoUrl" to p1Photo,
                    "partnerAPhotoUrl" to p2Photo,
                    "partnerBPhotoUrl" to p1Photo,
                    "partner1ColorHex" to p2Color,
                    "partner2ColorHex" to p1Color,
                    "partnerAColorHex" to p2Color,
                    "partnerBColorHex" to p1Color,
                    "userUids" to swappedUids,
                    "updatedAt" to System.currentTimeMillis()
                )
                if (p1Id != null || p2Id != null) {
                    updateMap["partner1Id"] = p2Id
                    updateMap["partner2Id"] = p1Id
                }

                val nonNullUpdates = updateMap.filterValues { it != null }.mapValues { it.value as Any }
                transaction.update(spaceRef, nonNullUpdates)
            }.await()

            // Also swap memory photo slots so photos stay bound to the person
            val memoriesSnapshot = spaceRef.collection("memories").get().await()
            if (!memoriesSnapshot.isEmpty) {
                val batch = firestore.batch()
                for (doc in memoriesSnapshot.documents) {
                    val memA = doc.getString("partnerAImageUrl")
                    val memB = doc.getString("partnerBImageUrl")
                    if (memA != null || memB != null) {
                        batch.update(
                            doc.reference,
                            mapOf(
                                "partnerAImageUrl" to memB,
                                "partnerBImageUrl" to memA,
                                "imageUrl" to memB
                            )
                        )
                    }
                }
                batch.commit().await()
            }

            // Update local state
            _currentSpace.value?.let { current ->
                _currentSpace.value = current.copy(
                    partnerAName = current.partnerBName,
                    partnerBName = current.partnerAName,
                    partner1PhotoUrl = current.partner2PhotoUrl,
                    partner2PhotoUrl = current.partner1PhotoUrl,
                    partner1ColorHex = current.partner2ColorHex,
                    partner2ColorHex = current.partner1ColorHex
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOutUser() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            // Ignore
        }
        _currentUserProfile.value = null
    }
}
