package com.aistudio.couplebubble.qxztrw.data.repository

import com.aistudio.couplebubble.qxztrw.domain.repository.CoupleRepository
import com.aistudio.couplebubble.qxztrw.domain.repository.MemoryRepository
import com.aistudio.couplebubble.qxztrw.domain.repository.MilestoneRepository
import com.aistudio.couplebubble.qxztrw.domain.repository.SpaceRepository

/** Combines the Firebase implementations of the three couple repositories behind one [CoupleRepository]. */
class FirebaseCoupleRepository(
    spaces: SpaceRepository,
    memories: MemoryRepository,
    milestones: MilestoneRepository
) : CoupleRepository,
    SpaceRepository by spaces,
    MemoryRepository by memories,
    MilestoneRepository by milestones
