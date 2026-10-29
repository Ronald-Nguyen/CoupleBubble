package com.aistudio.couplebubble.qxztrw.data.demo

import com.aistudio.couplebubble.qxztrw.data.local.LocalImageStore
import com.aistudio.couplebubble.qxztrw.domain.error.InvalidPairingCodeException
import com.aistudio.couplebubble.qxztrw.domain.error.PartnerNotConnectedException
import com.aistudio.couplebubble.qxztrw.domain.error.PartnerSlotUnknownException
import com.aistudio.couplebubble.qxztrw.domain.error.SpaceUnavailableException
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.MemoryPhotoChange
import com.aistudio.couplebubble.qxztrw.domain.model.PairingCode
import com.aistudio.couplebubble.qxztrw.domain.model.PairingCodes
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile
import com.aistudio.couplebubble.qxztrw.domain.model.editableRole
import com.aistudio.couplebubble.qxztrw.domain.repository.CoupleRepository
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The space, moments and custom milestones of the demo mode, kept in memory for the life of the process; no
 * Firebase is touched. The user is always signed in as [DemoSeed.USER_UID]. Entering any 6-digit code joins
 * the seeded sample space; sharing or copying the own code lets the simulated partner join a new, empty space
 * after [partnerJoinDelayMillis]. Photos are only stored locally.
 */
class DemoCoupleRepository(
    private val localImageStore: LocalImageStore,
    private val scope: CoroutineScope,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val partnerJoinDelayMillis: Long = PARTNER_JOIN_DELAY_MILLIS
) : CoupleRepository {

    private val _currentSpace = MutableStateFlow<CoupleSpace?>(null)
    override val currentSpace: StateFlow<CoupleSpace?> = _currentSpace.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(UserProfile(uid = DemoSeed.USER_UID))
    override val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    override val currentAuthUid: String = DemoSeed.USER_UID

    // Content per space id; the seeded space gets its sample content the first time it is opened
    private val memoriesBySpace = MutableStateFlow<Map<String, List<Memory>>>(emptyMap())
    private val milestonesBySpace = MutableStateFlow<Map<String, List<CustomMilestone>>>(emptyMap())

    private var pendingCode: String? = null
    private var partnerJoinJob: Job? = null

    override suspend fun ensureSignedIn(): String = DemoSeed.USER_UID

    // Space, pairing and session

    override suspend fun generateNewPairingCode(): PairingCode {
        partnerJoinJob?.cancel()
        val code = PairingCodes.generate()
        pendingCode = PairingCodes.normalize(code)
        return PairingCode(code = code, totalValidSeconds = PAIRING_CODE_VALID_SECONDS)
    }

    override suspend fun onPairingCodeShared(code: String) {
        val clean = PairingCodes.normalize(code)
        if (clean != pendingCode || partnerJoinJob?.isActive == true) return
        partnerJoinJob = scope.launch {
            delay(partnerJoinDelayMillis)
            if (_currentSpace.value == null && pendingCode == clean) {
                pendingCode = null
                enter(DemoSeed.joinedSpace(clean))
            }
        }
    }

    override suspend fun connectWithCode(code: String): Result<CoupleSpace> {
        delay(NETWORK_DELAY_MILLIS)
        if (!PairingCodes.isValid(code)) return Result.failure(InvalidPairingCodeException())
        partnerJoinJob?.cancel()
        pendingCode = null
        return Result.success(enter(seededSpace()))
    }

    override suspend fun openDemoSpace(): CoupleSpace = enter(seededSpace())

    override suspend fun restoreSession(coupleId: String): Result<CoupleSpace> =
        if (coupleId == DemoSeed.SPACE_ID) {
            Result.success(enter(seededSpace()))
        } else {
            // Spaces created from an own code only live in memory and are gone after a restart
            Result.failure(IllegalStateException("Demo space $coupleId is gone"))
        }

    override suspend fun restoreSessionForUser(uid: String): Result<CoupleSpace?> = Result.success(null)

    override suspend fun disconnect() {
        _currentSpace.value = null
        _currentUserProfile.update { it?.copy(coupleId = null) }
    }

    override suspend fun disconnectCouple(coupleId: String): Result<Unit> {
        disconnect()
        return Result.success(Unit)
    }

    private fun seededSpace(): CoupleSpace {
        val day = today()
        memoriesBySpace.update { if (DemoSeed.SPACE_ID in it) it else it + (DemoSeed.SPACE_ID to DemoSeed.memories(day)) }
        milestonesBySpace.update {
            if (DemoSeed.SPACE_ID in it) it else it + (DemoSeed.SPACE_ID to DemoSeed.customMilestones(day))
        }
        // Names, colors and photos changed earlier in this process stay
        return _currentSpace.value?.takeIf { it.id == DemoSeed.SPACE_ID } ?: DemoSeed.space(day)
    }

    private fun enter(space: CoupleSpace): CoupleSpace {
        _currentSpace.value = space
        _currentUserProfile.update { it?.copy(coupleId = space.id) }
        return space
    }

    private fun editSpace(coupleId: String, transform: (CoupleSpace) -> CoupleSpace): Result<Unit> {
        val space = _currentSpace.value?.takeIf { it.id == coupleId }
            ?: return Result.failure(SpaceUnavailableException())
        _currentSpace.value = transform(space)
        return Result.success(Unit)
    }

    override suspend fun updatePartnerNames(coupleId: String, partner1Name: String, partner2Name: String): Result<Unit> =
        editSpace(coupleId) { it.copy(partner1Name = partner1Name, partner2Name = partner2Name, isSetupComplete = true) }

    override suspend fun updateSpaceDetails(
        coupleId: String,
        partner1Name: String,
        partner2Name: String,
        anniversary: LocalDate
    ): Result<Unit> = editSpace(coupleId) {
        it.copy(
            partner1Name = partner1Name,
            partner2Name = partner2Name,
            anniversaryYear = anniversary.year,
            anniversaryMonth = anniversary.monthValue,
            anniversaryDay = anniversary.dayOfMonth,
            isSetupComplete = true
        )
    }

    override suspend fun updatePartnerColor(coupleId: String, isPartner1: Boolean, colorHex: String): Result<Unit> =
        editSpace(coupleId) { if (isPartner1) it.copy(partner1ColorHex = colorHex) else it.copy(partner2ColorHex = colorHex) }

    override suspend fun uploadProfilePhoto(coupleId: String, fallbackRole: PartnerRole?, imageBytes: ByteArray): Result<String> {
        val role = _currentSpace.value?.editableRole(DemoSeed.USER_UID, fallbackRole)
            ?: return Result.failure(PartnerSlotUnknownException())
        val url = localImageStore.saveProfilePhoto(coupleId, DemoSeed.USER_UID, imageBytes)
            ?: return Result.failure(IllegalStateException("Profile photo could not be stored"))
        return editSpace(coupleId) {
            if (role == PartnerRole.PARTNER_1) it.copy(partner1PhotoUrl = url) else it.copy(partner2PhotoUrl = url)
        }.map { url }
    }

    override suspend fun ensurePartnerSlot(coupleId: String, preferPartner2: Boolean) = Unit

    override suspend fun swapPartners(coupleId: String): Result<Unit> {
        delay(NETWORK_DELAY_MILLIS)
        val space = _currentSpace.value?.takeIf { it.id == coupleId } ?: return Result.failure(SpaceUnavailableException())
        if (space.partner1Id == null || space.partner2Id == null) return Result.failure(PartnerNotConnectedException())
        _currentSpace.value = space.copy(
            partner1Name = space.partner2Name,
            partner2Name = space.partner1Name,
            partner1PhotoUrl = space.partner2PhotoUrl,
            partner2PhotoUrl = space.partner1PhotoUrl,
            partner1ColorHex = space.partner2ColorHex,
            partner2ColorHex = space.partner1ColorHex,
            partner1Id = space.partner2Id,
            partner2Id = space.partner1Id,
            userUids = listOf(space.partner2Id, space.partner1Id)
        )
        editMemories(coupleId) { memories ->
            memories.map { it.copy(imageUrl = null, partner1ImageUrl = it.partner2ImageUrl, partner2ImageUrl = it.effectivePartner1Image) }
        }
        return Result.success(Unit)
    }

    // Google sign-in does not exist in the demo; the space never depends on it

    override suspend fun signInWithGoogleUser(
        uid: String,
        email: String?,
        displayName: String?,
        previousUid: String?
    ): Result<UserProfile> = Result.success(_currentUserProfile.value ?: UserProfile(uid = DemoSeed.USER_UID))

    override suspend fun linkCurrentUserToSpace(coupleId: String): Result<Unit> = Result.success(Unit)

    override suspend fun signOutUser() = Unit

    // Moments

    override fun getMemories(coupleId: String): Flow<List<Memory>> = memoriesBySpace.map { it[coupleId].orEmpty() }

    private fun editMemories(coupleId: String, transform: (List<Memory>) -> List<Memory>) {
        memoriesBySpace.update { spaces -> spaces + (coupleId to transform(spaces[coupleId].orEmpty()).sortedByDescending { it.date }) }
    }

    override suspend fun addMemory(
        coupleId: String,
        memory: Memory,
        fallbackRole: PartnerRole?,
        ownPhoto: MemoryPhotoChange
    ): Result<Memory> = saveMemory(coupleId, memory.copy(createdBy = DemoSeed.USER_UID), fallbackRole, ownPhoto, stored = null)

    override suspend fun updateMemory(
        coupleId: String,
        memory: Memory,
        fallbackRole: PartnerRole?,
        ownPhoto: MemoryPhotoChange
    ): Result<Memory> {
        val stored = memoriesBySpace.value[coupleId]?.firstOrNull { it.id == memory.id }
            ?: return Result.failure(NoSuchElementException("Memory ${memory.id} not found"))
        return saveMemory(coupleId, memory.copy(createdBy = stored.createdBy), fallbackRole, ownPhoto, stored)
    }

    /** Like the Firebase repository: photos come from the stored moment, only the user's own slot changes. */
    private suspend fun saveMemory(
        coupleId: String,
        memory: Memory,
        fallbackRole: PartnerRole?,
        ownPhoto: MemoryPhotoChange,
        stored: Memory?
    ): Result<Memory> {
        delay(NETWORK_DELAY_MILLIS)
        val id = memory.id.ifBlank { "demo_m_${UUID.randomUUID()}" }
        var saved = memory.copy(
            id = id,
            imageUrl = null,
            partner1ImageUrl = stored?.effectivePartner1Image,
            partner2ImageUrl = stored?.effectivePartner2Image,
            countsAsMilestone = stored?.countsAsMilestone ?: memory.countsAsMilestone
        )
        if (ownPhoto != MemoryPhotoChange.Keep) {
            val role = _currentSpace.value?.editableRole(DemoSeed.USER_UID, fallbackRole)
                ?: return Result.failure(PartnerSlotUnknownException())
            val url = (ownPhoto as? MemoryPhotoChange.Replace)?.let {
                localImageStore.saveMemoryImage(id, if (role == PartnerRole.PARTNER_1) "a" else "b", it.bytes)
            }
            saved = if (role == PartnerRole.PARTNER_1) saved.copy(partner1ImageUrl = url) else saved.copy(partner2ImageUrl = url)
        }
        editMemories(coupleId) { memories -> memories.filterNot { it.id == id } + saved }
        return Result.success(saved)
    }

    /** Adds a moment as the simulated partner, into the partner's own photo slot. */
    internal fun addPartnerMemory(coupleId: String, memory: Memory, photoUrl: String?) {
        val space = _currentSpace.value?.takeIf { it.id == coupleId } ?: return
        val partnerIsPartner1 = space.partner1Id == DemoSeed.PARTNER_UID
        val saved = memory.copy(
            id = "demo_m_${UUID.randomUUID()}",
            partner1ImageUrl = photoUrl.takeIf { partnerIsPartner1 },
            partner2ImageUrl = photoUrl.takeUnless { partnerIsPartner1 }
        )
        editMemories(coupleId) { it + saved }
    }

    override suspend fun deleteMemory(coupleId: String, memoryId: String): Result<Unit> {
        editMemories(coupleId) { memories -> memories.filterNot { it.id == memoryId } }
        return Result.success(Unit)
    }

    override suspend fun setMemoryCountsAsMilestone(coupleId: String, memoryId: String, enabled: Boolean): Result<Unit> {
        editMemories(coupleId) { memories -> memories.map { if (it.id == memoryId) it.copy(countsAsMilestone = enabled) else it } }
        return Result.success(Unit)
    }

    // Custom milestones

    override fun getCustomMilestones(coupleId: String): Flow<List<CustomMilestone>> =
        milestonesBySpace.map { it[coupleId].orEmpty() }

    private fun editMilestones(coupleId: String, transform: (List<CustomMilestone>) -> List<CustomMilestone>) {
        milestonesBySpace.update { spaces -> spaces + (coupleId to transform(spaces[coupleId].orEmpty()).sortedBy { it.date }) }
    }

    override suspend fun addCustomMilestone(coupleId: String, milestone: CustomMilestone): Result<CustomMilestone> {
        val saved = milestone.copy(id = milestone.id.ifBlank { "demo_cm_${UUID.randomUUID()}" })
        editMilestones(coupleId) { milestones -> milestones.filterNot { it.id == saved.id } + saved }
        return Result.success(saved)
    }

    override suspend fun setCustomMilestoneEnabled(coupleId: String, milestoneId: String, enabled: Boolean): Result<Unit> {
        editMilestones(coupleId) { milestones -> milestones.map { if (it.id == milestoneId) it.copy(enabled = enabled) else it } }
        return Result.success(Unit)
    }

    override suspend fun deleteCustomMilestone(coupleId: String, milestoneId: String): Result<Unit> {
        editMilestones(coupleId) { milestones -> milestones.filterNot { it.id == milestoneId } }
        return Result.success(Unit)
    }

    private companion object {
        const val PAIRING_CODE_VALID_SECONDS = 900
        const val PARTNER_JOIN_DELAY_MILLIS = 6_000L

        // A short pause so spinners and upload badges show up as they would online
        const val NETWORK_DELAY_MILLIS = 400L
    }
}
