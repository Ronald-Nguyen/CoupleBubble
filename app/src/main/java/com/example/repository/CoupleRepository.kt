package com.example.repository

import com.example.model.CoupleSpace
import com.example.model.PairingCode
import kotlinx.coroutines.flow.StateFlow

interface CoupleRepository {
    val currentSpace: StateFlow<CoupleSpace?>
    suspend fun generateNewPairingCode(): PairingCode
    suspend fun connectWithCode(code: String): Result<CoupleSpace>
    suspend fun disconnect()
    suspend fun openDemoSpace(): CoupleSpace
}
