package com.aistudio.couplebubble.qxztrw.data.remote.mapper

import com.aistudio.couplebubble.qxztrw.data.remote.FirestoreSchema.Space
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.SpaceDefaults

/**
 * Reads and writes `spaces/{id}`. Partner 1/2 values are mirrored into their legacy A/B aliases on every write,
 * and reads fall back to the alias for spaces written by older app versions.
 */
internal object SpaceMapper {

    private const val SPACE_ID_PREFIX = "space_"

    fun partner1Name(fields: Fields): String =
        fields.string(Space.PARTNER_1_NAME) ?: fields.string(Space.PARTNER_1_NAME_ALIAS) ?: SpaceDefaults.PARTNER_1_NAME

    fun partner2Name(fields: Fields): String =
        fields.string(Space.PARTNER_2_NAME) ?: fields.string(Space.PARTNER_2_NAME_ALIAS) ?: SpaceDefaults.PARTNER_2_NAME

    fun partner1ColorHex(fields: Fields): String = fields.string(Space.PARTNER_1_COLOR) ?: SpaceDefaults.PARTNER_1_COLOR_HEX
    fun partner2ColorHex(fields: Fields): String = fields.string(Space.PARTNER_2_COLOR) ?: SpaceDefaults.PARTNER_2_COLOR_HEX

    fun partner1PhotoUrl(fields: Fields): String? =
        fields.string(Space.PARTNER_1_PHOTO) ?: fields.string(Space.PARTNER_1_PHOTO_ALIAS)

    fun partner2PhotoUrl(fields: Fields): String? =
        fields.string(Space.PARTNER_2_PHOTO) ?: fields.string(Space.PARTNER_2_PHOTO_ALIAS)

    fun photoOf(fields: Fields, role: PartnerRole): String? =
        if (role == PartnerRole.PARTNER_1) partner1PhotoUrl(fields) else partner2PhotoUrl(fields)

    fun userUids(fields: Fields): List<String> = fields.strings(Space.USER_UIDS).orEmpty()

    /**
     * The bound `partner1Id` / `partner2Id`. Spaces from before the explicit IDs (both missing) take them from the
     * `userUids` order.
     */
    fun boundPartnerIds(fields: Fields): Pair<String?, String?> {
        val p1 = fields.string(Space.PARTNER_1_ID)
        val p2 = fields.string(Space.PARTNER_2_ID)
        if (p1 != null || p2 != null) return p1 to p2
        val uids = userUids(fields)
        return uids.getOrNull(0) to uids.getOrNull(1)
    }

    /** Whether [uid] must not join with the code: two others are already in the space, or another partner 2 is. */
    fun isJoinRefused(fields: Fields, uid: String?): Boolean {
        val members = fields.strings(Space.USER_UIDS) ?: fields.strings(Space.MEMBERS).orEmpty()
        val partner2Id = fields.string(Space.PARTNER_2_ID)
        val isMember = uid != null && (uid in members || uid == fields.string(Space.PARTNER_1_ID) || uid == partner2Id)
        val isFull = members.size >= 2 || (!partner2Id.isNullOrBlank() && partner2Id != uid)
        return isFull && !isMember
    }

    // Spaces from before the flag count as set up once someone changed the placeholder names
    private fun isSetupComplete(fields: Fields): Boolean = fields.bool(Space.IS_SETUP_COMPLETE)
        ?: (partner1Name(fields) != SpaceDefaults.PARTNER_1_NAME || partner2Name(fields) != SpaceDefaults.PARTNER_2_NAME)

    /** Only the membership fields, enough for [CoupleSpace.editableRole] inside a transaction. */
    fun members(fields: Fields): CoupleSpace = CoupleSpace(
        partner1Id = fields.string(Space.PARTNER_1_ID),
        partner2Id = fields.string(Space.PARTNER_2_ID),
        userUids = userUids(fields)
    )

    /** The space, or `null` once it was disconnected. */
    fun fromFields(id: String, fields: Fields): CoupleSpace? {
        if (fields.bool(Space.IS_ACTIVE) == false) return null
        val partner1 = partner1Name(fields)
        val partner2 = partner2Name(fields)
        return CoupleSpace(
            id = id,
            partner1Name = partner1,
            partner2Name = partner2,
            anniversaryYear = fields.int(Space.ANNIVERSARY_YEAR) ?: SpaceDefaults.ANNIVERSARY_YEAR,
            anniversaryMonth = fields.int(Space.ANNIVERSARY_MONTH) ?: SpaceDefaults.ANNIVERSARY_MONTH,
            anniversaryDay = fields.int(Space.ANNIVERSARY_DAY) ?: SpaceDefaults.ANNIVERSARY_DAY,
            anniversaryEpochMillis = fields.long(Space.ANNIVERSARY_EPOCH_MILLIS) ?: SpaceDefaults.ANNIVERSARY_EPOCH_MILLIS,
            isSetupComplete = isSetupComplete(fields),
            isActive = true,
            userUids = userUids(fields),
            partner1Id = fields.string(Space.PARTNER_1_ID),
            partner2Id = fields.string(Space.PARTNER_2_ID),
            partner1PhotoUrl = partner1PhotoUrl(fields),
            partner2PhotoUrl = partner2PhotoUrl(fields),
            partner1ColorHex = partner1ColorHex(fields),
            partner2ColorHex = partner2ColorHex(fields),
            // Older spaces may lack the field; their id still carries the code (`space_482913`)
            pairingCode = fields.string(Space.PAIRING_CODE) ?: pairingCodeFromId(id).orEmpty()
        )
    }

    fun pairingCodeFromId(id: String): String? =
        id.removePrefix(SPACE_ID_PREFIX).takeIf { id.startsWith(SPACE_ID_PREFIX) && it.length == 6 && it.all(Char::isDigit) }

    fun spaceIdForCode(cleanCode: String): String = SPACE_ID_PREFIX + cleanCode

    fun names(partner1Name: String, partner2Name: String): Map<String, Any> = mapOf(
        Space.PARTNER_1_NAME to partner1Name,
        Space.PARTNER_2_NAME to partner2Name,
        Space.PARTNER_1_NAME_ALIAS to partner1Name,
        Space.PARTNER_2_NAME_ALIAS to partner2Name
    )

    fun anniversary(year: Int, month: Int, day: Int): Map<String, Any> = mapOf(
        Space.ANNIVERSARY_YEAR to year,
        Space.ANNIVERSARY_MONTH to month,
        Space.ANNIVERSARY_DAY to day
    )

    /**
     * What joining space [id] with [cleanCode] writes: the names, anniversary and setup state already stored (or
     * the defaults for a space that does not exist yet), now active.
     */
    fun joined(id: String, cleanCode: String, fields: Fields, now: Long): Map<String, Any> =
        names(partner1Name(fields), partner2Name(fields)) +
            anniversary(
                fields.int(Space.ANNIVERSARY_YEAR) ?: SpaceDefaults.ANNIVERSARY_YEAR,
                fields.int(Space.ANNIVERSARY_MONTH) ?: SpaceDefaults.ANNIVERSARY_MONTH,
                fields.int(Space.ANNIVERSARY_DAY) ?: SpaceDefaults.ANNIVERSARY_DAY
            ) +
            mapOf(
                Space.ID to id,
                Space.PAIRING_CODE to cleanCode,
                Space.IS_SETUP_COMPLETE to isSetupComplete(fields),
                Space.IS_ACTIVE to true,
                Space.PAIRED_AT to now
            )

    /** The placeholder space a fresh pairing code listens on until a partner joins. */
    fun pendingSpace(spaceId: String, cleanCode: String, now: Long): Map<String, Any> = mapOf(
        Space.ID to spaceId,
        Space.PAIRING_CODE to cleanCode,
        Space.PARTNER_1_NAME to SpaceDefaults.PARTNER_1_NAME,
        Space.PARTNER_2_NAME to SpaceDefaults.PARTNER_2_NAME,
        Space.IS_SETUP_COMPLETE to false,
        Space.IS_ACTIVE to false,
        Space.CREATED_AT to now
    ) + anniversary(SpaceDefaults.ANNIVERSARY_YEAR, SpaceDefaults.ANNIVERSARY_MONTH, SpaceDefaults.ANNIVERSARY_DAY)

    /** [url] may be `FieldValue.delete()`, as the role swap needs. */
    fun photo(role: PartnerRole, url: Any): Map<String, Any> {
        val isPartner1 = role == PartnerRole.PARTNER_1
        return mapOf(
            (if (isPartner1) Space.PARTNER_1_PHOTO else Space.PARTNER_2_PHOTO) to url,
            (if (isPartner1) Space.PARTNER_1_PHOTO_ALIAS else Space.PARTNER_2_PHOTO_ALIAS) to url
        )
    }

    fun color(isPartner1: Boolean, colorHex: String): Map<String, Any> = mapOf(
        (if (isPartner1) Space.PARTNER_1_COLOR else Space.PARTNER_2_COLOR) to colorHex,
        (if (isPartner1) Space.PARTNER_1_COLOR_ALIAS else Space.PARTNER_2_COLOR_ALIAS) to colorHex
    )
}
