package com.aistudio.couplebubble.qxztrw.domain.model

import java.security.SecureRandom
import java.util.Random

/** The pairing code is the only way into a half-full space, so it is drawn from a cryptographic source. */
object PairingCodes {
    private val secureRandom = SecureRandom()

    /** Six random digits formatted as `482-913`; [random] is only replaced in tests. */
    fun generate(random: Random = secureRandom): String {
        val digits = (100_000 + random.nextInt(900_000)).toString()
        return "${digits.take(3)}-${digits.drop(3)}"
    }

    /** The stored form of a typed or pasted code: `482-913` and ` 482913 ` both become `482913`. */
    fun normalize(code: String): String = code.filterNot { it == '-' || it.isWhitespace() }

    /** Whether [code] (after [normalize]) is exactly six digits. */
    fun isValid(code: String): Boolean = normalize(code).let { it.length == 6 && it.all { c -> c in '0'..'9' } }
}
