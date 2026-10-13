package com.aistudio.couplebubble.qxztrw

import com.aistudio.couplebubble.qxztrw.model.PairingCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class PairingCodesTest {

    @Test
    fun generatedCodesHaveSixDigitsWithHyphen() {
        val random = Random(42)
        repeat(1_000) {
            val code = PairingCodes.generate(random)
            assertTrue(code, Regex("^[1-9][0-9]{2}-[0-9]{3}$").matches(code))
        }
    }

    @Test
    fun boundsOfTheRandomRangeStaySixDigits() {
        val lowest = object : Random() {
            override fun nextInt(bound: Int) = 0
        }
        val highest = object : Random() {
            override fun nextInt(bound: Int) = bound - 1
        }
        assertEquals("100-000", PairingCodes.generate(lowest))
        assertEquals("999-999", PairingCodes.generate(highest))
    }
}
