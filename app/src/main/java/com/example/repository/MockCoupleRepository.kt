package com.example.repository

import com.example.model.CoupleSpace
import com.example.model.PairingCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockCoupleRepository : CoupleRepository {

    private val _currentSpace = MutableStateFlow<CoupleSpace?>(null)
    override val currentSpace: StateFlow<CoupleSpace?> = _currentSpace.asStateFlow()

    private var activeGeneratedCode = "BLU-789"

    override suspend fun generateNewPairingCode(): PairingCode {
        delay(200)
        // Can generate fresh recognizable codes
        val prefixes = listOf("BLU", "LUV", "JOY", "SUN", "DUO")
        val number = (100..999).random()
        activeGeneratedCode = "${prefixes.random()}-$number"
        return PairingCode(code = activeGeneratedCode, totalValidSeconds = 900)
    }

    override suspend fun connectWithCode(code: String): Result<CoupleSpace> {
        // Realistic simulated network latency
        delay(800)

        val cleanCode = code.replace("-", "").trim().uppercase()

        // Validation per user requirements: accept BLU-789 or any 6-character code
        if (cleanCode.length != 6) {
            return Result.failure(
                IllegalArgumentException("Der Code muss genau 6 Zeichen lang sein (z. B. BLU-789).")
            )
        }

        // Successfully paired space with preloaded realistic anniversary (~1 year and 3 months ago: June 25, 2025)
        val pairedSpace = CoupleSpace(
            id = "couple_space_active",
            partner1Name = "Alex",
            partner2Name = "Sam",
            anniversaryYear = 2025,
            anniversaryMonth = 6,
            anniversaryDay = 25
        )

        _currentSpace.value = pairedSpace
        return Result.success(pairedSpace)
    }

    override suspend fun disconnect() {
        delay(300)
        _currentSpace.value = null
    }

    override suspend fun openDemoSpace(): CoupleSpace {
        val demoSpace = CoupleSpace(
            id = "demo_space",
            partner1Name = "Alex",
            partner2Name = "Sam",
            anniversaryYear = 2025,
            anniversaryMonth = 6,
            anniversaryDay = 25
        )
        _currentSpace.value = demoSpace
        return demoSpace
    }
}
