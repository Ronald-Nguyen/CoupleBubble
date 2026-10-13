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
            // Spaces from before the flag count as set up once someone changed the placeholder names
            isSetupComplete = fields.bool(Space.IS_SETUP_COMPLETE)
                ?: (partner1 != SpaceDefaults.PARTNER_1_NAME || partner2 != SpaceDefaults.PARTNER_2_NAME),
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
