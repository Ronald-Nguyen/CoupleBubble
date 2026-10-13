package com.aistudio.couplebubble.qxztrw.domain.repository

/** Everything the couple screens need; split by concern so each part can be implemented and faked on its own. */
interface CoupleRepository : SpaceRepository, MemoryRepository, MilestoneRepository
