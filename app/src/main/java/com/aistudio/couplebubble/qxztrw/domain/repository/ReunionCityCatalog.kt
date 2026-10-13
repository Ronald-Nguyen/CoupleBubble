package com.aistudio.couplebubble.qxztrw.domain.repository

import com.aistudio.couplebubble.qxztrw.domain.model.ReunionCity

/** The offline list of cities for the reunion distance; loaded on first use, never sent anywhere. */
interface ReunionCityCatalog {
    suspend fun suggestions(query: String, limit: Int = 5): List<ReunionCity>
    suspend fun find(query: String): ReunionCity?
}
