package com.aistudio.couplebubble.qxztrw.data.fake

import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.MemoryPhotoChange
import com.aistudio.couplebubble.qxztrw.domain.model.PairingCode
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile
import com.aistudio.couplebubble.qxztrw.domain.model.editableRole
import com.aistudio.couplebubble.qxztrw.domain.model.roleOf
import com.aistudio.couplebubble.qxztrw.domain.repository.CoupleRepository
import com.aistudio.couplebubble.qxztrw.domain.error.InvalidPairingCodeException
import com.aistudio.couplebubble.qxztrw.domain.error.PartnerNotConnectedException
import com.aistudio.couplebubble.qxztrw.domain.error.PartnerSlotUnknownException
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    /** Makes the space, memory and milestone writes fail, as refused Firestore writes would. */
    var failWrites = false

    private fun refused(): Result<Nothing> = Result.failure(IllegalStateException("PERMISSION_DENIED"))

    override suspend fun addMemory(
        coupleId: String,
        memory: Memory,
        fallbackRole: PartnerRole?,
        ownPhoto: MemoryPhotoChange
    ): Result<Memory> {
        delay(100.milliseconds)
        if (failWrites) return refused()
        return saveMemory(coupleId, memory, fallbackRole, ownPhoto, stored = null)
    }

    override suspend fun updateMemory(
        coupleId: String,
        memory: Memory,
        fallbackRole: PartnerRole?,
        ownPhoto: MemoryPhotoChange
    ): Result<Memory> {
        delay(100.milliseconds)
        if (failWrites) return refused()
        return saveMemory(coupleId, memory, fallbackRole, ownPhoto, stored = _memories.value.firstOrNull { it.id == memory.id })
    }

    /** Like Firestore: photos come from the stored moment, and only the caller's own slot (by UID) can change. */
    private fun saveMemory(
        coupleId: String,
        memory: Memory,
        fallbackRole: PartnerRole?,
        ownPhoto: MemoryPhotoChange,
        stored: Memory?
    ): Result<Memory> {
        val id = memory.id.ifBlank { "m_${System.nanoTime()}" }
        var saved = memory.copy(
            id = id,
            imageUrl = null,
            partner1ImageUrl = stored?.effectivePartner1Image,
            partner2ImageUrl = stored?.effectivePartner2Image,
            // Like Firestore: only a new moment takes the switch along, edits leave it to setMemoryCountsAsMilestone
            countsAsMilestone = stored?.countsAsMilestone ?: memory.countsAsMilestone
        )
        if (ownPhoto != MemoryPhotoChange.Keep) {
            val role = _currentSpace.value?.editableRole(_currentUserProfile.value?.uid, fallbackRole)
                ?: return Result.failure(PartnerSlotUnknownException())
            val slot = if (role == PartnerRole.PARTNER_1) "a" else "b"
            val url = if (ownPhoto is MemoryPhotoChange.Replace) {
                "https://firebasestorage.googleapis.com/v0/b/mock/o/couples%2F$coupleId%2Fmemories%2F${id}_$slot.jpg?alt=media"
            } else {
                null
            }
            saved = if (role == PartnerRole.PARTNER_1) saved.copy(partner1ImageUrl = url) else saved.copy(partner2ImageUrl = url)
        }
        _memories.value = (listOf(saved) + _memories.value.filterNot { it.id == id }).sortedByDescending { it.date }
        return Result.success(saved)
    }

    /** Makes [deleteMemory] fail, as a refused Firestore write would. */
    var failDeleteMemory = false

    /** The moments as stored, without any local hiding by a ViewModel. */
    val storedMemories: List<Memory> get() = _memories.value

    override suspend fun deleteMemory(coupleId: String, memoryId: String): Result<Unit> {
        delay(100.milliseconds)
        if (failDeleteMemory) return Result.failure(IllegalStateException("delete refused"))
        _memories.value = _memories.value.filterNot { it.id == memoryId }
        return Result.success(Unit)
    }

    override suspend fun setMemoryCountsAsMilestone(coupleId: String, memoryId: String, enabled: Boolean): Result<Unit> {
        if (failWrites) return refused()
        _memories.value = _memories.value.map { if (it.id == memoryId) it.copy(countsAsMilestone = enabled) else it }
        return Result.success(Unit)
    }

    override fun getCustomMilestones(coupleId: String): Flow<List<CustomMilestone>> = _customMilestones.asStateFlow()

    override suspend fun addCustomMilestone(coupleId: String, milestone: CustomMilestone): Result<CustomMilestone> {
        if (failWrites) return refused()
        val saved = milestone.copy(id = milestone.id.ifBlank { "cm_${System.currentTimeMillis()}" })
        _customMilestones.value = (_customMilestones.value.filterNot { it.id == saved.id } + saved).sortedBy { it.date }
        return Result.success(saved)
    }

    override suspend fun setCustomMilestoneEnabled(coupleId: String, milestoneId: String, enabled: Boolean): Result<Unit> {
        if (failWrites) return refused()
        _customMilestones.value = _customMilestones.value.map {
            if (it.id == milestoneId) it.copy(enabled = enabled) else it
        }
        return Result.success(Unit)
    }

    override suspend fun deleteCustomMilestone(coupleId: String, milestoneId: String): Result<Unit> {
        if (failWrites) return refused()
        _customMilestones.value = _customMilestones.value.filterNot { it.id == milestoneId }
        return Result.success(Unit)
    }

    override suspend fun updatePartnerNames(
        coupleId: String,
        partner1Name: String,
        partner2Name: String
    ): Result<Unit> {
        delay(100.milliseconds)
        if (failWrites) return refused()
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
        anniversary: LocalDate
    ): Result<Unit> {
        delay(100.milliseconds)
        if (failWrites) return refused()
        _currentSpace.value?.let { current ->
            _currentSpace.value = current.copy(
                partner1Name = partner1Name,
                partner2Name = partner2Name,
                anniversaryYear = anniversary.year,
                anniversaryMonth = anniversary.monthValue,
                anniversaryDay = anniversary.dayOfMonth,
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

    /** Runs between resolving nothing and writing, to simulate a role swap while the upload is in flight. */
    internal var beforeProfilePhotoWrite: (() -> Unit)? = null

    internal val ensuredSlots = mutableListOf<Pair<String, Boolean>>()

    override suspend fun uploadProfilePhoto(
        coupleId: String,
        fallbackRole: PartnerRole?,
        imageBytes: ByteArray
    ): Result<String> {
        delay(50.milliseconds)
        beforeProfilePhotoWrite?.invoke()
        val space = _currentSpace.value ?: return Result.failure(PartnerSlotUnknownException())
        // Like Firestore: the slot is resolved from the UID when writing, never taken from the caller
        val role = space.editableRole(_currentUserProfile.value?.uid, fallbackRole)
            ?: return Result.failure(PartnerSlotUnknownException())
        val partnerId = if (role == PartnerRole.PARTNER_1) "partner1" else "partner2"
        val mockUrl = "https://firebasestorage.googleapis.com/v0/b/mock/o/couples%2F$coupleId%2Fprofiles%2F$partnerId.jpg?alt=media&t=${System.currentTimeMillis()}"
        _currentSpace.value = if (role == PartnerRole.PARTNER_1) {
            space.copy(partner1PhotoUrl = mockUrl)
        } else {
            space.copy(partner2PhotoUrl = mockUrl)
        }
        return Result.success(mockUrl)
    }

    override suspend fun ensurePartnerSlot(coupleId: String, preferPartner2: Boolean) {
        ensuredSlots += coupleId to preferPartner2
        val uid = _currentUserProfile.value?.uid ?: return
        val space = _currentSpace.value ?: return
        if (space.roleOf(uid) != null) return
        _currentSpace.value = when {
            space.partner2Id == null && (preferPartner2 || space.partner1Id != null) -> space.copy(partner2Id = uid)
            space.partner1Id == null -> space.copy(partner1Id = uid)
            else -> space
        }.let { it.copy(userUids = listOfNotNull(it.partner1Id, it.partner2Id)) }
    }

    override suspend fun updatePartnerColor(
        coupleId: String,
        isPartner1: Boolean,
        colorHex: String
    ): Result<Unit> {
        delay(50.milliseconds)
        if (failWrites) return refused()
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
