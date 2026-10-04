package com.aistudio.couplebubble.qxztrw.repository

import com.aistudio.couplebubble.qxztrw.model.SpaceDefaults
import com.google.firebase.firestore.DocumentSnapshot

/** Collection and field names as stored in Firestore. Changing a value breaks existing data. */
internal object FirestoreSchema {
    const val SPACES = "spaces"
    const val USERS = "users"
    const val MEMORIES = "memories"
    const val MILESTONES = "milestones"
    const val NOTES = "notes"
    const val SETTINGS = "settings"
    const val NOTE_LABELS_DOC = "noteLabels"

    /** Fields of `spaces/{id}`. Partner 1/2 values are mirrored into their legacy A/B aliases. */
    object Space {
        const val ID = "id"
        const val PAIRING_CODE = "pairingCode"
        const val PARTNER_1_NAME = "partnerAName"
        const val PARTNER_2_NAME = "partnerBName"
        const val PARTNER_1_NAME_ALIAS = "partner1Name"
        const val PARTNER_2_NAME_ALIAS = "partner2Name"
        const val PARTNER_1_PHOTO = "partner1PhotoUrl"
        const val PARTNER_2_PHOTO = "partner2PhotoUrl"
        const val PARTNER_1_PHOTO_ALIAS = "partnerAPhotoUrl"
        const val PARTNER_2_PHOTO_ALIAS = "partnerBPhotoUrl"
        const val PARTNER_1_COLOR = "partner1ColorHex"
        const val PARTNER_2_COLOR = "partner2ColorHex"
        const val PARTNER_1_COLOR_ALIAS = "partnerAColorHex"
        const val PARTNER_2_COLOR_ALIAS = "partnerBColorHex"
        const val PARTNER_1_ID = "partner1Id"
        const val PARTNER_2_ID = "partner2Id"
        const val USER_UIDS = "userUids"
        const val MEMBERS = "members"
        const val ANNIVERSARY_YEAR = "anniversaryYear"
        const val ANNIVERSARY_MONTH = "anniversaryMonth"
        const val ANNIVERSARY_DAY = "anniversaryDay"
        const val ANNIVERSARY_EPOCH_MILLIS = "anniversaryEpochMillis"
        const val IS_SETUP_COMPLETE = "isSetupComplete"
        const val IS_ACTIVE = "isActive"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val PAIRED_AT = "pairedAt"
        const val DISCONNECTED_AT = "disconnectedAt"
    }

    /** Fields of `spaces/{id}/memories/{memoryId}`. */
    object Memory {
        const val ID = "id"
        const val TITLE = "title"
        const val DATE = "date"
        const val NOTE = "note"
        const val LEGACY_IMAGE_URL = "imageUrl"
        const val PARTNER_1_IMAGE = "partnerAImageUrl"
        const val PARTNER_2_IMAGE = "partnerBImageUrl"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }

    /** Fields of `users/{uid}`. */
    object User {
        const val UID = "uid"
        const val EMAIL = "email"
        const val DISPLAY_NAME = "displayName"
        const val COUPLE_ID = "coupleId"
        const val UPDATED_AT = "updatedAt"
    }
}

internal fun DocumentSnapshot.partner1Name(): String =
    getString(FirestoreSchema.Space.PARTNER_1_NAME)
        ?: getString(FirestoreSchema.Space.PARTNER_1_NAME_ALIAS)
        ?: SpaceDefaults.PARTNER_1_NAME

internal fun DocumentSnapshot.partner2Name(): String =
    getString(FirestoreSchema.Space.PARTNER_2_NAME)
        ?: getString(FirestoreSchema.Space.PARTNER_2_NAME_ALIAS)
        ?: SpaceDefaults.PARTNER_2_NAME

internal fun DocumentSnapshot.partner1ColorHex(): String =
    getString(FirestoreSchema.Space.PARTNER_1_COLOR) ?: SpaceDefaults.PARTNER_1_COLOR_HEX

internal fun DocumentSnapshot.partner2ColorHex(): String =
    getString(FirestoreSchema.Space.PARTNER_2_COLOR) ?: SpaceDefaults.PARTNER_2_COLOR_HEX

internal fun DocumentSnapshot.partner1PhotoUrl(): String? =
    getString(FirestoreSchema.Space.PARTNER_1_PHOTO) ?: getString(FirestoreSchema.Space.PARTNER_1_PHOTO_ALIAS)

internal fun DocumentSnapshot.partner2PhotoUrl(): String? =
    getString(FirestoreSchema.Space.PARTNER_2_PHOTO) ?: getString(FirestoreSchema.Space.PARTNER_2_PHOTO_ALIAS)

internal fun DocumentSnapshot.userUids(): List<String> =
    (get(FirestoreSchema.Space.USER_UIDS) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

/**
 * Maps stored memory fields to (partner 1, partner 2) photo slots.
 * The legacy `imageUrl` used to mirror `A ?: B`, so it only counts as partner 1 for old single-photo moments,
 * and a partner 1 slot that equals partner 2 is a copy produced by that mirror.
 */
internal fun resolveMemorySlots(legacyUrl: String?, partner1Url: String?, partner2Url: String?): Pair<String?, String?> {
    val first = partner1Url ?: legacyUrl.takeIf { partner2Url == null }
    return (if (first != null && first == partner2Url) null else first) to partner2Url
}
