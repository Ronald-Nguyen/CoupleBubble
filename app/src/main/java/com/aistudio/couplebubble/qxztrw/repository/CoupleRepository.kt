package com.aistudio.couplebubble.qxztrw.repository

import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.PairingCode
import com.aistudio.couplebubble.qxztrw.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CoupleRepository {
    val currentSpace: StateFlow<CoupleSpace?>
    val currentUserProfile: StateFlow<UserProfile?>

    fun getMemories(coupleId: String): Flow<List<Memory>>
    suspend fun addMemory(
        coupleId: String,
        memory: Memory,
        imageABytes: ByteArray? = null,
        imageBBytes: ByteArray? = null,
    ): Result<Memory>
    suspend fun updateMemory(
        coupleId: String,
        memory: Memory,
        imageABytes: ByteArray? = null,
        imageBBytes: ByteArray? = null,
    ): Result<Memory>
    suspend fun deleteMemory(coupleId: String, memoryId: String): Result<Unit>
    suspend fun updatePartnerNames(coupleId: String, partnerAName: String, partnerBName: String): Result<Unit>
    suspend fun updateSpaceDetails(
        coupleId: String,
        partnerAName: String,
        partnerBName: String,
        anniversaryYear: Int,
        anniversaryMonth: Int,
        anniversaryDay: Int,
    ): Result<Unit>
    suspend fun generateNewPairingCode(): PairingCode
    suspend fun connectWithCode(code: String): Result<CoupleSpace>
    suspend fun restoreSession(coupleId: String): Result<CoupleSpace>
    suspend fun restoreSessionForUser(uid: String): Result<CoupleSpace?>
    suspend fun disconnect()
    suspend fun disconnectCouple(coupleId: String): Result<Unit>
    suspend fun openDemoSpace(): CoupleSpace
    suspend fun listenToPairingCode(code: String) {}

    // Google Sign-In & Data Anchor
    suspend fun signInWithGoogleUser(
        uid: String,
        email: String?,
        displayName: String?,
        previousUid: String? = null,
    ): Result<UserProfile>
    suspend fun linkCurrentUserToSpace(coupleId: String): Result<Unit>
    suspend fun uploadProfilePhoto(coupleId: String, isPartner1: Boolean, imageUri: android.net.Uri): Result<String>
    suspend fun uploadProfilePhoto(coupleId: String, isPartner1: Boolean, imageBytes: ByteArray): Result<String>
    suspend fun updatePartnerColor(coupleId: String, isPartner1: Boolean, colorHex: String): Result<Unit>
    suspend fun swapPartners(coupleId: String): Result<Unit>
    @Suppress("unused")
    suspend fun joinCoupleSpace(pairingCode: String): Result<CoupleSpace> = connectWithCode(pairingCode)
    suspend fun signOutUser()
}

/** A role swap needs both partners bound to the space; otherwise one person's data would move without them. */
class PartnerNotConnectedException : IllegalStateException("Partner is not connected yet")

internal fun Throwable.unwrapPartnerNotConnected(): Throwable =
    generateSequence(this) { it.cause }.firstOrNull { it is PartnerNotConnectedException } ?: this
