package com.aistudio.couplebubble.qxztrw.domain.model

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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

    @Test
    fun normalizeDropsHyphensAndWhitespace() {
        assertEquals("482913", PairingCodes.normalize("482-913"))
        assertEquals("482913", PairingCodes.normalize(" 482 913\n"))
    }

    @Test
    fun onlySixDigitsAreValid() {
        assertTrue(PairingCodes.isValid("482-913"))
        assertTrue(PairingCodes.isValid("482913"))
        assertFalse(PairingCodes.isValid("48291"))
        assertFalse(PairingCodes.isValid("4829134"))
        assertFalse(PairingCodes.isValid("48a-913"))
        assertFalse(PairingCodes.isValid(""))
    }
}
