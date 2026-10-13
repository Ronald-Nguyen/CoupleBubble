package com.aistudio.couplebubble.qxztrw.domain.repository

import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import kotlinx.coroutines.flow.Flow

/** Custom milestones both partners share (`spaces/{id}/milestones`); they can be switched off, never deleted. */
interface MilestoneRepository {
    fun getCustomMilestones(coupleId: String): Flow<List<CustomMilestone>>
    suspend fun addCustomMilestone(coupleId: String, milestone: CustomMilestone): Result<CustomMilestone>
    suspend fun setCustomMilestoneEnabled(coupleId: String, milestoneId: String, enabled: Boolean): Result<Unit>
}
