package com.aistudio.couplebubble.qxztrw.domain.error

/**
 * Every failure the app answers with its own message or dialog. Being sealed, a `when` over it must name each
 * case, so a new error cannot silently fall into a generic message.
 */
sealed class CoupleException(message: String) : IllegalStateException(message)

/** A role swap needs both partners bound to the space; otherwise one person's data would move without them. */
class PartnerNotConnectedException : CoupleException("Partner is not connected yet")

/** The signed-in user is not bound to a partner slot, so it is unknown whose photo would change. */
class PartnerSlotUnknownException : CoupleException("Current user has no partner slot")

/** Joining failed because the space already has two members; the pairing UI answers it with a dedicated dialog. */
class SpaceFullException : CoupleException("Space already has two members")

/** Firestore did not answer in time and has no cached copy; the session stays and loads once online. */
class SpaceUnavailableException : CoupleException("Space could not be loaded")

/** The entered pairing code is not exactly six digits. */
class InvalidPairingCodeException : CoupleException("Pairing code must have exactly 6 digits")

/** Firestore wraps exceptions thrown inside transactions; this finds the typed one again. */
inline fun <reified T : Throwable> Throwable.findCause(): T? =
    generateSequence(this) { it.cause }.filterIsInstance<T>().firstOrNull()
