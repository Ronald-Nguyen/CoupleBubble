package com.aistudio.couplebubble.qxztrw.repository

import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.PairingCode
import com.aistudio.couplebubble.qxztrw.model.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class MockCoupleRepository : CoupleRepository {

    private val _currentSpace = MutableStateFlow<CoupleSpace?>(null)
    override val currentSpace: StateFlow<CoupleSpace?> = _currentSpace.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    override val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    private val _memories = MutableStateFlow<List<Memory>>(
        listOf(
            Memory(
                id = "m1",
                title = "Erster Tag zusammen",
                date = LocalDate.of(2025, 6, 25),
                note = "Der schönste Spaziergang am See. Beide haben wir diesen Moment festgehalten.",
                partnerAImageUrl = "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2?w=800",
                partnerBImageUrl = "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=800"
            ),
            Memory(
                id = "m2",
                title = "Erste gemeinsame Reise",
                date = LocalDate.of(2025, 9, 18),
                note = "Magischer Sommerurlaub am Meer.",
                partnerAImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800",
                partnerBImageUrl = null
            ),
            Memory(
                id = "m3",
                title = "Zusammengezogen",
                date = LocalDate.of(2026, 2, 12),
                note = "Unsere erste gemeinsame Wohnung eingerichtet.",
                partnerAImageUrl = null,
                partnerBImageUrl = null
            )
        )
    )

    private var activeGeneratedCode = "BLU-789"

    override fun getMemories(coupleId: String): Flow<List<Memory>> = _memories.asStateFlow()

    override suspend fun addMemory(
        coupleId: String,
        memory: Memory,
        imageABytes: ByteArray?,
        imageBBytes: ByteArray?
    ): Result<Memory> {
        delay(100)
        val newMemories = listOf(memory) + _memories.value
        _memories.value = newMemories.sortedByDescending { it.date }
        return Result.success(memory)
    }

    override suspend fun updateMemory(
        coupleId: String,
        memory: Memory,
        imageABytes: ByteArray?,
        imageBBytes: ByteArray?
    ): Result<Memory> {
        delay(100)
        val currentList = _memories.value.filterNot { it.id == memory.id }
        _memories.value = (listOf(memory) + currentList).sortedByDescending { it.date }
        return Result.success(memory)
    }

    override suspend fun deleteMemory(coupleId: String, memoryId: String): Result<Unit> {
        delay(100)
        _memories.value = _memories.value.filterNot { it.id == memoryId }
        return Result.success(Unit)
    }

    override suspend fun updatePartnerNames(
        coupleId: String,
        partnerAName: String,
        partnerBName: String
    ): Result<Unit> {
        delay(100)
        _currentSpace.value?.let { current ->
            _currentSpace.value = current.copy(
                partnerAName = partnerAName,
                partnerBName = partnerBName,
                isSetupComplete = true
            )
        }
        return Result.success(Unit)
    }

    override suspend fun updateSpaceDetails(
        coupleId: String,
        partnerAName: String,
        partnerBName: String,
        anniversaryYear: Int,
        anniversaryMonth: Int,
        anniversaryDay: Int
    ): Result<Unit> {
        delay(100)
        _currentSpace.value?.let { current ->
            _currentSpace.value = current.copy(
                partnerAName = partnerAName,
                partnerBName = partnerBName,
                anniversaryYear = anniversaryYear,
                anniversaryMonth = anniversaryMonth,
                anniversaryDay = anniversaryDay,
                isSetupComplete = true
            )
        }
        return Result.success(Unit)
    }

    override suspend fun generateNewPairingCode(): PairingCode {
        delay(100)
        val prefixes = listOf("BLU", "LUV", "JOY", "SUN", "DUO")
        val number = (100..999).random()
        activeGeneratedCode = "${prefixes.random()}-$number"
        return PairingCode(code = activeGeneratedCode, totalValidSeconds = 900)
    }

    override suspend fun connectWithCode(code: String): Result<CoupleSpace> {
        delay(100)
        val cleanCode = code.replace("-", "").trim().uppercase()

        if (cleanCode.length != 6) {
            return Result.failure(
                IllegalArgumentException("Der Code muss genau 6 Zeichen lang sein (z. B. BLU-789).")
            )
        }

        val pairedSpace = CoupleSpace(
            id = "couple_space_active",
            partnerAName = "Alex",
            partnerBName = "Sam",
            anniversaryYear = 2025,
            anniversaryMonth = 6,
            anniversaryDay = 25,
            isSetupComplete = true,
            isActive = true
        )

        _currentSpace.value = pairedSpace
        return Result.success(pairedSpace)
    }

    override suspend fun restoreSession(coupleId: String): Result<CoupleSpace> {
        val restoredSpace = CoupleSpace(
            id = coupleId,
            partnerAName = "Alex",
            partnerBName = "Sam",
            anniversaryYear = 2025,
            anniversaryMonth = 6,
            anniversaryDay = 25,
            isSetupComplete = true,
            isActive = true
        )
        _currentSpace.value = restoredSpace
        return Result.success(restoredSpace)
    }

    override suspend fun restoreSessionForUser(uid: String): Result<CoupleSpace?> {
        val profile = _currentUserProfile.value
        if (profile != null && !profile.coupleId.isNullOrBlank()) {
            val spaceRes = restoreSession(profile.coupleId)
            return Result.success(spaceRes.getOrNull())
        }
        return Result.success(null)
    }

    override suspend fun disconnect() {
        _currentSpace.value = null
    }

    override suspend fun disconnectCouple(coupleId: String): Result<Unit> {
        _currentSpace.value = null
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
        _currentSpace.value = demoSpace
        return demoSpace
    }

    override suspend fun signInWithGoogleUser(
        uid: String,
        email: String?,
        displayName: String?
    ): Result<UserProfile> {
        delay(100)
        val profile = UserProfile(
            uid = uid,
            email = email ?: "user@gmail.com",
            displayName = displayName ?: "Google User",
            coupleId = _currentSpace.value?.id
        )
        _currentUserProfile.value = profile
        return Result.success(profile)
    }

    override suspend fun linkCurrentUserToSpace(coupleId: String): Result<Unit> {
        delay(100)
        val current = _currentUserProfile.value
        if (current != null) {
            _currentUserProfile.value = current.copy(coupleId = coupleId)
        }
        return Result.success(Unit)
    }

    override suspend fun signOutUser() {
        _currentUserProfile.value = null
    }
}
