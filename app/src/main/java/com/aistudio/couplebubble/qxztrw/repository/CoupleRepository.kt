package com.aistudio.couplebubble.qxztrw.repository

import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.PairingCode
import com.aistudio.couplebubble.qxztrw.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CoupleRepository {
    val currentSpace: StateFlow<CoupleSpace?>
    val currentUserProfile: StateFlow<UserProfile?>

    /** UID of the signed-in Firebase user, or `null` when nobody is signed in or Firebase is unavailable. */
    val currentAuthUid: String?

    /** Signs in anonymously if nobody is signed in yet; returns the resulting UID, if any. */
    suspend fun ensureSignedIn(): String?

    fun getMemories(coupleId: String): Flow<List<Memory>>
    suspend fun addMemory(
        coupleId: String,
        memory: Memory,
        image1Bytes: ByteArray? = null,
        image2Bytes: ByteArray? = null,
    ): Result<Memory>
    suspend fun updateMemory(
        coupleId: String,
        memory: Memory,
        image1Bytes: ByteArray? = null,
        image2Bytes: ByteArray? = null,
    ): Result<Memory>
    suspend fun deleteMemory(coupleId: String, memoryId: String): Result<Unit>
    fun getCustomMilestones(coupleId: String): Flow<List<CustomMilestone>>
    suspend fun addCustomMilestone(coupleId: String, milestone: CustomMilestone): Result<CustomMilestone>
    suspend fun deleteCustomMilestone(coupleId: String, milestoneId: String): Result<Unit>
    suspend fun updatePartnerNames(coupleId: String, partner1Name: String, partner2Name: String): Result<Unit>

    /** Always succeeds: a failed write keeps the local state and syncs later. */
    suspend fun updateSpaceDetails(
        coupleId: String,
        partner1Name: String,
        partner2Name: String,
        anniversaryYear: Int,
        anniversaryMonth: Int,
        anniversaryDay: Int,
    ): Result<Unit>
    suspend fun generateNewPairingCode(): PairingCode
    suspend fun connectWithCode(code: String): Result<CoupleSpace>
    suspend fun restoreSession(coupleId: String): Result<CoupleSpace>
    suspend fun restoreSessionForUser(uid: String): Result<CoupleSpace?>
    suspend fun disconnect()

    /** Always succeeds: the local session ends even if the space could not be marked inactive. */
    suspend fun disconnectCouple(coupleId: String): Result<Unit>
    suspend fun openDemoSpace(): CoupleSpace
    suspend fun listenToPairingCode(code: String) {}

    // Google Sign-In & Data Anchor

    /** Always succeeds: network errors keep the local profile. */
    suspend fun signInWithGoogleUser(
        uid: String,
        email: String?,
        displayName: String?,
        previousUid: String? = null,
    ): Result<UserProfile>

    /** Always succeeds: the anchor is retried on the next restore or sign-in. */
    suspend fun linkCurrentUserToSpace(coupleId: String): Result<Unit>
    suspend fun uploadProfilePhoto(coupleId: String, isPartner1: Boolean, imageBytes: ByteArray): Result<String>
    suspend fun updatePartnerColor(coupleId: String, isPartner1: Boolean, colorHex: String): Result<Unit>
    suspend fun swapPartners(coupleId: String): Result<Unit>
    suspend fun signOutUser()
}

/** A role swap needs both partners bound to the space; otherwise one person's data would move without them. */
class PartnerNotConnectedException : IllegalStateException("Partner is not connected yet")

/** Joining failed because the space already has two members; the pairing UI answers it with a dedicated dialog. */
class SpaceFullException : IllegalStateException("Space already has two members")

/** Firestore did not answer in time and has no cached copy; the session stays and loads once online. */
class SpaceUnavailableException : IllegalStateException("Space could not be loaded")

/** The entered pairing code is not exactly six digits. */
class InvalidPairingCodeException : IllegalArgumentException("Pairing code must have exactly 6 digits")

internal fun Throwable.unwrapPartnerNotConnected(): Throwable =
    generateSequence(this) { it.cause }.firstOrNull { it is PartnerNotConnectedException } ?: this
