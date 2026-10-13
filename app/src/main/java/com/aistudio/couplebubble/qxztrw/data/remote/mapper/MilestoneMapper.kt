package com.aistudio.couplebubble.qxztrw.data.remote.mapper

import com.aistudio.couplebubble.qxztrw.data.remote.FirestoreSchema
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import java.time.LocalDate

/** Reads and writes `spaces/{id}/milestones/{milestoneId}`. */
internal object MilestoneMapper {

    fun fromFields(id: String, fields: Fields): CustomMilestone? {
        val title = fields.string(FirestoreSchema.Milestone.TITLE) ?: return null
        val date = fields.string(FirestoreSchema.Milestone.DATE)?.let {
            try {
                LocalDate.parse(it)
            } catch (_: Exception) {
                null
            }
        } ?: return null
        return CustomMilestone(
            id = id,
            title = title,
            date = date,
            recurringYearly = fields.bool(FirestoreSchema.Milestone.RECURRING_YEARLY) ?: false,
            enabled = fields.bool(FirestoreSchema.Milestone.ENABLED) ?: true
        )
    }

    fun toFields(milestone: CustomMilestone, createdAt: Long): Map<String, Any> = mapOf(
        FirestoreSchema.Milestone.TITLE to milestone.title,
        FirestoreSchema.Milestone.DATE to milestone.date.toString(),
        FirestoreSchema.Milestone.RECURRING_YEARLY to milestone.recurringYearly,
        FirestoreSchema.Milestone.ENABLED to milestone.enabled,
        FirestoreSchema.Milestone.CREATED_AT to createdAt
    )
}
