package com.aistudio.couplebubble.qxztrw.data.remote.mapper

import com.aistudio.couplebubble.qxztrw.data.local.ImageUrls
import com.aistudio.couplebubble.qxztrw.data.remote.FirestoreSchema
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import java.time.LocalDate

/** Reads `spaces/{id}/memories/{memoryId}`; photo slots are stored with their legacy A/B names. */
internal object MemoryMapper {

    fun fromFields(id: String, fields: Fields, today: LocalDate = LocalDate.now()): Memory? = try {
        val dateStr = fields.string(FirestoreSchema.Memory.DATE)
        val (partner1Url, partner2Url) = resolveMemorySlots(
            legacyUrl = ImageUrls.sanitize(fields.string(FirestoreSchema.Memory.LEGACY_IMAGE_URL)),
            partner1Url = ImageUrls.sanitize(fields.string(FirestoreSchema.Memory.PARTNER_1_IMAGE)),
            partner2Url = ImageUrls.sanitize(fields.string(FirestoreSchema.Memory.PARTNER_2_IMAGE)),
        )
        Memory(
            id = id,
            title = fields.string(FirestoreSchema.Memory.TITLE).orEmpty(),
            date = if (!dateStr.isNullOrEmpty()) LocalDate.parse(dateStr) else today,
            note = fields.string(FirestoreSchema.Memory.NOTE).orEmpty(),
            partner1ImageUrl = partner1Url,
            partner2ImageUrl = partner2Url,
            createdBy = fields.string(FirestoreSchema.Memory.CREATED_BY),
            milestoneKey = fields.string(FirestoreSchema.Memory.MILESTONE_KEY)
        )
    } catch (_: Exception) {
        null
    }

    /** The stored (partner 1, partner 2) photo URLs, unsanitized, as read inside transactions. */
    fun storedSlots(fields: Fields): Pair<String?, String?> = resolveMemorySlots(
        legacyUrl = fields.string(FirestoreSchema.Memory.LEGACY_IMAGE_URL),
        partner1Url = fields.string(FirestoreSchema.Memory.PARTNER_1_IMAGE),
        partner2Url = fields.string(FirestoreSchema.Memory.PARTNER_2_IMAGE),
    )

    fun slotField(role: PartnerRole): String =
        if (role == PartnerRole.PARTNER_1) FirestoreSchema.Memory.PARTNER_1_IMAGE else FirestoreSchema.Memory.PARTNER_2_IMAGE

    /** Every photo URL a memory document may still reference, for cleaning up Storage on delete. */
    fun allPhotoUrls(fields: Fields): List<String> = listOf(
        FirestoreSchema.Memory.PARTNER_1_IMAGE,
        FirestoreSchema.Memory.PARTNER_2_IMAGE,
        FirestoreSchema.Memory.LEGACY_IMAGE_URL
    ).mapNotNull { fields.string(it) }.distinct()
}

/**
 * Maps stored memory fields to (partner 1, partner 2) photo slots.
 * The legacy `imageUrl` used to mirror `A ?: B`, so it only counts as partner 1 for old single-photo moments,
 * and a partner 1 slot that equals partner 2 is a copy produced by that mirror.
 */
internal fun resolveMemorySlots(legacyUrl: String?, partner1Url: String?, partner2Url: String?): Pair<String?, String?> {
    val first = partner1Url ?: legacyUrl.takeIf { partner2Url == null }
    return (if (first != null && first == partner2Url) null else first) to partner2Url
}
