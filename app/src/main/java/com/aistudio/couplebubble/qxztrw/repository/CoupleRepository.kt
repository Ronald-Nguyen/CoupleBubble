package com.aistudio.couplebubble.qxztrw.repository

import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.PairingCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CoupleRepository {
    val currentSpace: StateFlow<CoupleSpace?>
    fun getMemories(coupleId: String): Flow<List<Memory>>
    suspend fun addMemory(coupleId: String, memory: Memory, imageBytes: ByteArray? = null): Result<Memory>
    suspend fun updateMemory(coupleId: String, memory: Memory, imageBytes: ByteArray? = null): Result<Memory>
    suspend fun deleteMemory(coupleId: String, memoryId: String): Result<Unit>
    suspend fun updatePartnerNames(coupleId: String, partnerAName: String, partnerBName: String): Result<Unit>
    suspend fun generateNewPairingCode(): PairingCode
    suspend fun connectWithCode(code: String): Result<CoupleSpace>
    suspend fun restoreSession(coupleId: String): Result<CoupleSpace>
    suspend fun disconnect()
    suspend fun disconnectCouple(coupleId: String): Result<Unit>
    suspend fun openDemoSpace(): CoupleSpace
}
