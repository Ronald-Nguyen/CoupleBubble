package com.aistudio.couplebubble.qxztrw.model

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
}
