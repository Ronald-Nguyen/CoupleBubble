package com.aistudio.couplebubble.qxztrw.domain.repository

import com.aistudio.couplebubble.qxztrw.domain.error.PartnerSlotUnknownException
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.PairingCode
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow

/** Sign-in, pairing and the shared space document: names, anniversary, partner slots, photos and colors. */
interface SpaceRepository {
    val currentSpace: StateFlow<CoupleSpace?>
    val currentUserProfile: StateFlow<UserProfile?>

    /** UID of the signed-in Firebase user, or `null` when nobody is signed in or Firebase is unavailable. */
    val currentAuthUid: String?

    /** Signs in anonymously if nobody is signed in yet; returns the resulting UID, if any. */
    suspend fun ensureSignedIn(): String?

    suspend fun updatePartnerNames(coupleId: String, partner1Name: String, partner2Name: String): Result<Unit>

    /** Always succeeds: a failed write keeps the local state and syncs later. */
    suspend fun updateSpaceDetails(
        coupleId: String,
        partner1Name: String,
        partner2Name: String,
        anniversary: LocalDate,
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
    /**
     * Saves the signed-in user's own profile photo. The slot is resolved from the auth UID against the partner
     * IDs when the write happens (not when the photo was picked), so a role swap in between cannot redirect it;
     * [fallbackRole] is only used in a space without members. Fails with [PartnerSlotUnknownException] if the
     * UID is not bound to a slot.
     */
    suspend fun uploadProfilePhoto(coupleId: String, fallbackRole: PartnerRole?, imageBytes: ByteArray): Result<String>

    /** Binds the signed-in user to a free slot if they are not a member yet (e.g. an earlier claim ran offline). */
    suspend fun ensurePartnerSlot(coupleId: String, preferPartner2: Boolean)
    suspend fun updatePartnerColor(coupleId: String, isPartner1: Boolean, colorHex: String): Result<Unit>
    suspend fun swapPartners(coupleId: String): Result<Unit>
    suspend fun signOutUser()
}
