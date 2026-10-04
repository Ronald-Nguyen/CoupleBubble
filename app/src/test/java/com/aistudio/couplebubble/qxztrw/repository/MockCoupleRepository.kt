package com.aistudio.couplebubble.qxztrw.repository

import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.PairingCode
import com.aistudio.couplebubble.qxztrw.model.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

open class MockCoupleRepository : CoupleRepository {

    private val _currentSpace = MutableStateFlow<CoupleSpace?>(null)
    override val currentSpace: StateFlow<CoupleSpace?> = _currentSpace.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    override val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    private val _memories = MutableStateFlow(
        listOf(
            Memory(
                id = "m1",
                title = "Erster Tag zusammen",
                date = LocalDate.of(2025, 6, 25),
                note = "Der schönste Spaziergang am See. Beide haben wir diesen Moment festgehalten.",
                partner1ImageUrl = "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2?w=800",
                partner2ImageUrl = "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=800",
            ),
            Memory(
                id = "m2",
                title = "Erste gemeinsame Reise",
                date = LocalDate.of(2025, 9, 18),
                note = "Magischer Sommerurlaub am Meer.",
                partner1ImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800",
                partner2ImageUrl = null,
            ),
            Memory(
                id = "m3",
                title = "Zusammengezogen",
                date = LocalDate.of(2026, 2, 12),
                note = "Unsere erste gemeinsame Wohnung eingerichtet.",
                partner1ImageUrl = null,
                partner2ImageUrl = null,
            ),
        )
    )

    private val _customMilestones = MutableStateFlow<List<CustomMilestone>>(emptyList())

    private var activeGeneratedCode = "482-913"

    override val currentAuthUid: String? = null

    override suspend fun ensureSignedIn(): String? = null

    override fun getMemories(coupleId: String): Flow<List<Memory>> = _memories.asStateFlow()

    override suspend fun addMemory(
        coupleId: String,
        memory: Memory,
        image1Bytes: ByteArray?,
        image2Bytes: ByteArray?
    ): Result<Memory> {
        delay(100.milliseconds)
        val urlA = if (image1Bytes?.isNotEmpty() == true) {
            "https://firebasestorage.googleapis.com/v0/b/mock/o/couples%2F$coupleId%2Fmemories%2F${memory.id}_a.jpg?alt=media"
        } else memory.partner1ImageUrl
        val urlB = if (image2Bytes?.isNotEmpty() == true) {
            "https://firebasestorage.googleapis.com/v0/b/mock/o/couples%2F$coupleId%2Fmemories%2F${memory.id}_b.jpg?alt=media"
        } else memory.partner2ImageUrl

        val savedMemory = memory.copy(
            imageUrl = null,
            partner1ImageUrl = urlA,
            partner2ImageUrl = urlB
        )
        val newMemories = listOf(savedMemory) + _memories.value
        _memories.value = newMemories.sortedByDescending { it.date }
        return Result.success(savedMemory)
    }

    override suspend fun updateMemory(
        coupleId: String,
        memory: Memory,
        image1Bytes: ByteArray?,
        image2Bytes: ByteArray?
    ): Result<Memory> {
        delay(100.milliseconds)
        val urlA = if (image1Bytes?.isNotEmpty() == true) {
            "https://firebasestorage.googleapis.com/v0/b/mock/o/couples%2F$coupleId%2Fmemories%2F${memory.id}_a.jpg?alt=media"
        } else memory.partner1ImageUrl
        val urlB = if (image2Bytes?.isNotEmpty() == true) {
            "https://firebasestorage.googleapis.com/v0/b/mock/o/couples%2F$coupleId%2Fmemories%2F${memory.id}_b.jpg?alt=media"
        } else memory.partner2ImageUrl

        val savedMemory = memory.copy(
            imageUrl = null,
            partner1ImageUrl = urlA,
            partner2ImageUrl = urlB
        )
        val currentList = _memories.value.filterNot { it.id == memory.id }
        _memories.value = (listOf(savedMemory) + currentList).sortedByDescending { it.date }
        return Result.success(savedMemory)
    }

    override suspend fun deleteMemory(coupleId: String, memoryId: String): Result<Unit> {
        delay(100.milliseconds)
        _memories.value = _memories.value.filterNot { it.id == memoryId }
        return Result.success(Unit)
    }

    override fun getCustomMilestones(coupleId: String): Flow<List<CustomMilestone>> = _customMilestones.asStateFlow()

    override suspend fun addCustomMilestone(coupleId: String, milestone: CustomMilestone): Result<CustomMilestone> {
        val saved = milestone.copy(id = milestone.id.ifBlank { "cm_${System.currentTimeMillis()}" })
        _customMilestones.value = (_customMilestones.value.filterNot { it.id == saved.id } + saved).sortedBy { it.date }
        return Result.success(saved)
    }

    override suspend fun deleteCustomMilestone(coupleId: String, milestoneId: String): Result<Unit> {
        _customMilestones.value = _customMilestones.value.filterNot { it.id == milestoneId }
        return Result.success(Unit)
    }

    override suspend fun updatePartnerNames(
        coupleId: String,
        partner1Name: String,
        partner2Name: String
    ): Result<Unit> {
        delay(100.milliseconds)
        _currentSpace.value?.let { current ->
            _currentSpace.value = current.copy(
                partner1Name = partner1Name,
                partner2Name = partner2Name,
                isSetupComplete = true
            )
        }
        return Result.success(Unit)
    }

    override suspend fun updateSpaceDetails(
        coupleId: String,
        partner1Name: String,
        partner2Name: String,
        anniversaryYear: Int,
        anniversaryMonth: Int,
        anniversaryDay: Int
    ): Result<Unit> {
        delay(100.milliseconds)
        _currentSpace.value?.let { current ->
            _currentSpace.value = current.copy(
                partner1Name = partner1Name,
                partner2Name = partner2Name,
                anniversaryYear = anniversaryYear,
                anniversaryMonth = anniversaryMonth,
                anniversaryDay = anniversaryDay,
                isSetupComplete = true
            )
        }
        return Result.success(Unit)
    }

    override suspend fun generateNewPairingCode(): PairingCode {
        delay(100.milliseconds)
        val digits = (100000..999999).random().toString()
        activeGeneratedCode = "${digits.take(3)}-${digits.drop(3)}"
        return PairingCode(code = activeGeneratedCode, totalValidSeconds = 900)
    }

    override suspend fun connectWithCode(code: String): Result<CoupleSpace> {
        delay(100.milliseconds)
        val cleanCode = code.replace("-", "").trim().uppercase()

        if (cleanCode.length != 6 || !cleanCode.all { it in '0'..'9' }) {
            return Result.failure(InvalidPairingCodeException())
        }

        val pairedSpace = CoupleSpace(
            id = "couple_space_active",
            partner1Name = "Alex",
            partner2Name = "Sam",
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
            partner1Name = "Alex",
            partner2Name = "Sam",
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
        val coupleId = _currentUserProfile.value?.coupleId
        if (!coupleId.isNullOrBlank()) {
            val spaceRes = restoreSession(coupleId)
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
            partner1Name = "Alex",
            partner2Name = "Sam",
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
        displayName: String?,
        previousUid: String?
    ): Result<UserProfile> {
        delay(100.milliseconds)
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
        delay(100.milliseconds)
        _currentUserProfile.value?.let { current ->
            _currentUserProfile.value = current.copy(coupleId = coupleId)
        }
        return Result.success(Unit)
    }

    override suspend fun uploadProfilePhoto(
        coupleId: String,
        isPartner1: Boolean,
        imageBytes: ByteArray
    ): Result<String> {
        delay(50.milliseconds)
        val partnerId = if (isPartner1) "partner1" else "partner2"
        val mockUrl = "https://firebasestorage.googleapis.com/v0/b/mock/o/couples%2F$coupleId%2Fprofiles%2F$partnerId.jpg?alt=media&t=${System.currentTimeMillis()}"
        _currentSpace.value = if (isPartner1) {
            _currentSpace.value?.copy(partner1PhotoUrl = mockUrl)
        } else {
            _currentSpace.value?.copy(partner2PhotoUrl = mockUrl)
        }
        return Result.success(mockUrl)
    }

    override suspend fun updatePartnerColor(
        coupleId: String,
        isPartner1: Boolean,
        colorHex: String
    ): Result<Unit> {
        delay(50.milliseconds)
        _currentSpace.value = if (isPartner1) {
            _currentSpace.value?.copy(partner1ColorHex = colorHex)
        } else {
            _currentSpace.value?.copy(partner2ColorHex = colorHex)
        }
        return Result.success(Unit)
    }

    override suspend fun swapPartners(coupleId: String): Result<Unit> {
        delay(50.milliseconds)
        val space = _currentSpace.value
        if (space != null && (space.partner1Id == null) != (space.partner2Id == null)) {
            return Result.failure(PartnerNotConnectedException())
        }
        _currentSpace.value?.let { current ->
            _currentSpace.value = current.copy(
                partner1Name = current.partner2Name,
                partner2Name = current.partner1Name,
                partner1PhotoUrl = current.partner2PhotoUrl,
                partner2PhotoUrl = current.partner1PhotoUrl,
                partner1ColorHex = current.partner2ColorHex,
                partner2ColorHex = current.partner1ColorHex,
                partner1Id = current.partner2Id,
                partner2Id = current.partner1Id,
                userUids = current.userUids.reversed()
            )
        }
        _memories.value = _memories.value.map { mem ->
            mem.copy(
                imageUrl = null,
                partner1ImageUrl = mem.partner2ImageUrl,
                partner2ImageUrl = mem.effectivePartner1Image
            )
        }
        return Result.success(Unit)
    }

    override suspend fun signOutUser() {
        _currentUserProfile.value = null
    }

    internal fun setStateForTesting(space: CoupleSpace?, profile: UserProfile?) {
        _currentSpace.value = space
        _currentUserProfile.value = profile
    }
}
