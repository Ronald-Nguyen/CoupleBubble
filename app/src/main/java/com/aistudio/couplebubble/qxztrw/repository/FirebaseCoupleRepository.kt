package com.aistudio.couplebubble.qxztrw.repository

import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.PairingCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
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
import java.util.UUID

class FirebaseCoupleRepository : CoupleRepository {

    private val _currentSpace = MutableStateFlow<CoupleSpace?>(null)
    override val currentSpace: StateFlow<CoupleSpace?> = _currentSpace.asStateFlow()

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

                val updatedSpace = CoupleSpace(
                    id = snapshot.id,
                    partnerAName = partnerA,
                    partnerBName = partnerB,
                    anniversaryYear = snapshot.getLong("anniversaryYear")?.toInt() ?: 2025,
                    anniversaryMonth = snapshot.getLong("anniversaryMonth")?.toInt() ?: 6,
                    anniversaryDay = snapshot.getLong("anniversaryDay")?.toInt() ?: 25,
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
                        Memory(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            date = date,
                            note = doc.getString("note") ?: "",
                            imageUrl = doc.getString("imageUrl")
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
        imageBytes: ByteArray?
    ): Result<Memory> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore uninitialized"))
        val memoryId = if (memory.id.isNotBlank()) memory.id else UUID.randomUUID().toString()

        var uploadedUrl = memory.imageUrl
        if (imageBytes != null && imageBytes.isNotEmpty()) {
            try {
                val storageRef = storage?.reference
                    ?.child("spaces")
                    ?.child(coupleId)
                    ?.child("memories")
                    ?.child("$memoryId.jpg")

                if (storageRef != null) {
                    val uploadTask = storageRef.putBytes(imageBytes).await()
                    uploadedUrl = storageRef.downloadUrl.await().toString()
                }
            } catch (e: Exception) {
                // If storage upload fails, proceed without image or propagate
            }
        }

        val memoryData = mapOf(
            "id" to memoryId,
            "title" to memory.title,
            "date" to memory.date.toString(),
            "note" to memory.note,
            "imageUrl" to uploadedUrl,
            "createdAt" to System.currentTimeMillis()
        )

        return try {
            firestore.collection("spaces")
                .document(coupleId)
                .collection("memories")
                .document(memoryId)
                .set(memoryData)
                .await()

            Result.success(memory.copy(id = memoryId, imageUrl = uploadedUrl))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMemory(
        coupleId: String,
        memory: Memory,
        imageBytes: ByteArray?
    ): Result<Memory> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore uninitialized"))
        var uploadedUrl = memory.imageUrl
        if (imageBytes != null && imageBytes.isNotEmpty()) {
            try {
                val storageRef = storage?.reference
                    ?.child("spaces")
                    ?.child(coupleId)
                    ?.child("memories")
                    ?.child("${memory.id}.jpg")

                if (storageRef != null) {
                    storageRef.putBytes(imageBytes).await()
                    uploadedUrl = storageRef.downloadUrl.await().toString()
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
            "imageUrl" to uploadedUrl,
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
            Result.success(memory.copy(imageUrl = uploadedUrl))
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
                    "partner2Name" to partnerBName
                )
            ).await()
            _currentSpace.value = _currentSpace.value?.copy(
                partnerAName = partnerAName,
                partnerBName = partnerBName
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateNewPairingCode(): PairingCode {
        val prefixes = listOf("BLU", "LUV", "JOY", "SUN", "DUO")
        val number = (100..999).random()
        val generatedCode = "${prefixes.random()}-$number"

        try {
            db?.collection("pairing_codes")?.document(generatedCode)?.set(
                mapOf(
                    "code" to generatedCode,
                    "createdAt" to System.currentTimeMillis(),
                    "validSeconds" to 900
                )
            )?.await()
        } catch (e: Exception) {
            // Fallback or log if Firebase is uninitialized
        }

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
                val querySnapshot = withTimeoutOrNull(2000L) {
                    firestore.collection("spaces")
                        .whereEqualTo("pairingCode", cleanCode)
                        .get()
                        .await()
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents.first()
                    val partnerA = doc.getString("partnerAName") ?: doc.getString("partner1Name") ?: "Alex"
                    val partnerB = doc.getString("partnerBName") ?: doc.getString("partner2Name") ?: "Sam"
                    val space = CoupleSpace(
                        id = doc.id,
                        partnerAName = partnerA,
                        partnerBName = partnerB,
                        anniversaryYear = doc.getLong("anniversaryYear")?.toInt() ?: 2025,
                        anniversaryMonth = doc.getLong("anniversaryMonth")?.toInt() ?: 6,
                        anniversaryDay = doc.getLong("anniversaryDay")?.toInt() ?: 25,
                        isActive = doc.getBoolean("isActive") ?: true
                    )
                    _currentSpace.value = space
                    listenToSpaceChanges(space.id)
                    return Result.success(space)
                }
            }
        } catch (e: Exception) {
            // Fallback if network or Firebase unavailable
        }

        val pairedSpace = CoupleSpace(
            id = "couple_space_active",
            partnerAName = "Alex",
            partnerBName = "Sam",
            anniversaryYear = 2025,
            anniversaryMonth = 6,
            anniversaryDay = 25,
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
                        isActive = isActive
                    )
                    _currentSpace.value = space
                    listenToSpaceChanges(space.id)
                    return Result.success(space)
                }
            }
        } catch (e: Exception) {
            // If offline, attempt fallback with space ID
        }

        val space = CoupleSpace(
            id = coupleId,
            partnerAName = "Alex",
            partnerBName = "Sam",
            anniversaryYear = 2025,
            anniversaryMonth = 6,
            anniversaryDay = 25,
            isActive = true
        )
        _currentSpace.value = space
        listenToSpaceChanges(space.id)
        return Result.success(space)
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
}
