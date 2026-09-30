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
            null
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

                val updatedSpace = CoupleSpace(
                    id = snapshot.id,
                    partnerAName = partnerA,
                    partnerBName = partnerB,
                    anniversaryYear = snapshot.getLong("anniversaryYear")?.toInt() ?: 2025,
                    anniversaryMonth = snapshot.getLong("anniversaryMonth")?.toInt() ?: 6,
                    anniversaryDay = snapshot.getLong("anniversaryDay")?.toInt() ?: 25,
                    anniversaryEpochMillis = snapshot.getLong("anniversaryEpochMillis") ?: 1750800000000L,
                    isSetupComplete = isSetupComplete,
                    isActive = isActive
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
                        val legacyUrl = doc.getString("imageUrl")
                        val partnerAUrl = doc.getString("partnerAImageUrl") ?: legacyUrl
                        val partnerBUrl = doc.getString("partnerBImageUrl")
                        Memory(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            date = date,
                            note = doc.getString("note") ?: "",
                            imageUrl = partnerAUrl,
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

        var urlA = memory.partnerAImageUrl ?: memory.imageUrl
        var urlB = memory.partnerBImageUrl

        if (imageABytes != null && imageABytes.isNotEmpty()) {
            try {
                val storageRef = storage?.reference
                    ?.child("spaces")
                    ?.child(coupleId)
                    ?.child("memories")
                    ?.child("${memoryId}_a.jpg")

                if (storageRef != null) {
                    storageRef.putBytes(imageABytes).await()
                    urlA = storageRef.downloadUrl.await().toString()
                }
            } catch (e: Exception) {
                // Proceed without uploaded image on error
            }
        }

        if (imageBBytes != null && imageBBytes.isNotEmpty()) {
            try {
                val storageRef = storage?.reference
                    ?.child("spaces")
                    ?.child(coupleId)
                    ?.child("memories")
                    ?.child("${memoryId}_b.jpg")

                if (storageRef != null) {
                    storageRef.putBytes(imageBBytes).await()
                    urlB = storageRef.downloadUrl.await().toString()
                }
            } catch (e: Exception) {
                // Proceed without uploaded image on error
            }
        }

        val memoryData = mapOf(
            "id" to memoryId,
            "title" to memory.title,
            "date" to memory.date.toString(),
            "note" to memory.note,
            "imageUrl" to urlA,
            "partnerAImageUrl" to urlA,
            "partnerBImageUrl" to urlB,
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
                    imageUrl = urlA,
                    partnerAImageUrl = urlA,
                    partnerBImageUrl = urlB
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
        var urlA = memory.partnerAImageUrl ?: memory.imageUrl
        var urlB = memory.partnerBImageUrl

        if (imageABytes != null && imageABytes.isNotEmpty()) {
            try {
                val storageRef = storage?.reference
                    ?.child("spaces")
                    ?.child(coupleId)
                    ?.child("memories")
                    ?.child("${memory.id}_a.jpg")

                if (storageRef != null) {
                    storageRef.putBytes(imageABytes).await()
                    urlA = storageRef.downloadUrl.await().toString()
                }
            } catch (e: Exception) {
                // Ignore storage upload exception or preserve existing
            }
        }

        if (imageBBytes != null && imageBBytes.isNotEmpty()) {
            try {
                val storageRef = storage?.reference
                    ?.child("spaces")
                    ?.child(coupleId)
                    ?.child("memories")
                    ?.child("${memory.id}_b.jpg")

                if (storageRef != null) {
                    storageRef.putBytes(imageBBytes).await()
                    urlB = storageRef.downloadUrl.await().toString()
                }
            } catch (e: Exception) {
                // Ignore storage upload exception or preserve existing
            }
        }

        val memoryData = mapOf(
            "id" to memory.id,
            "title" to memory.title,
            "date" to memory.date.toString(),
            "note" to memory.note,
            "imageUrl" to urlA,
            "partnerAImageUrl" to urlA,
            "partnerBImageUrl" to urlB,
            "updatedAt" to System.currentTimeMillis()
        )

        return try {
            withTimeoutOrNull(2000L) {
                firestore.collection("spaces")
                    .document(coupleId)
                    .collection("memories")
                    .document(memory.id)
                    .set(memoryData)
                    .await()
            }
            Result.success(
                memory.copy(
                    imageUrl = urlA,
                    partnerAImageUrl = urlA,
                    partnerBImageUrl = urlB
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMemory(coupleId: String, memoryId: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore uninitialized"))
        return try {
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

                val docId = if (snapshot != null && snapshot.exists()) snapshot.id else "space_$cleanCode"
                val partnerA = snapshot?.getString("partnerAName") ?: snapshot?.getString("partner1Name") ?: "Alex"
                val partnerB = snapshot?.getString("partnerBName") ?: snapshot?.getString("partner2Name") ?: "Sam"
                val isSetupComplete = snapshot?.getBoolean("isSetupComplete") ?: false

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

                val pairedSpace = CoupleSpace(
                    id = docId,
                    partnerAName = partnerA,
                    partnerBName = partnerB,
                    anniversaryYear = snapshot?.getLong("anniversaryYear")?.toInt() ?: 2025,
                    anniversaryMonth = snapshot?.getLong("anniversaryMonth")?.toInt() ?: 6,
                    anniversaryDay = snapshot?.getLong("anniversaryDay")?.toInt() ?: 25,
                    isSetupComplete = isSetupComplete,
                    isActive = true
                )
                _currentSpace.value = pairedSpace
                listenToSpaceChanges(pairedSpace.id)

                val currentUser = auth?.currentUser
                if (currentUser != null) {
                    linkCurrentUserToSpace(pairedSpace.id)
                }

                return Result.success(pairedSpace)
            }
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
                    val space = CoupleSpace(
                        id = doc.id,
                        partnerAName = partnerA,
                        partnerBName = partnerB,
                        anniversaryYear = doc.getLong("anniversaryYear")?.toInt() ?: 2025,
                        anniversaryMonth = doc.getLong("anniversaryMonth")?.toInt() ?: 6,
                        anniversaryDay = doc.getLong("anniversaryDay")?.toInt() ?: 25,
                        isSetupComplete = doc.getBoolean("isSetupComplete") ?: true,
                        isActive = isActive
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

    override suspend fun signOutUser() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            // Ignore
        }
        _currentUserProfile.value = null
    }
}
