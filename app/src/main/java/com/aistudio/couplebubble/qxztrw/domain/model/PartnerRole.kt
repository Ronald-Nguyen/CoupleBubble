package com.aistudio.couplebubble.qxztrw.domain.model

/** The role this device remembers for its user; only a fallback while the space has no explicit partner IDs. */
enum class PartnerRole(val storedValue: String) {
    PARTNER_1("1"),
    PARTNER_2("2");

    fun swapped(): PartnerRole = if (this == PARTNER_1) PARTNER_2 else PARTNER_1

    /** The slot letter in photo file names; kept as a/b so existing Storage and local names stay recognizable. */
    val fileSlot: String get() = if (this == PARTNER_1) "a" else "b"

    companion object {
        /**
         * The role used to decide the UI side while the space has no explicit IDs: anything but "2"
         * (including a missing value) shows this device as partner 1.
         */
        fun displayFallback(value: String?): PartnerRole = if (value == PARTNER_2.storedValue) PARTNER_2 else PARTNER_1

        /** Reads the stored preference for swapping; old installs stored "A"/"B". */
        fun fromStored(value: String?): PartnerRole? = when (value) {
            "1", "A" -> PARTNER_1
            "2", "B" -> PARTNER_2
            else -> null
        }
    }
}

/**
 * The slot [uid] is bound to: explicit partner IDs decide, `userUids` order is the fallback for old spaces.
 * `null` when the UID is unknown or not (yet) a member, so callers never guess whose data they change.
 */
fun CoupleSpace.roleOf(uid: String?): PartnerRole? = when {
    uid == null -> null
    uid == partner1Id -> PartnerRole.PARTNER_1
    uid == partner2Id -> PartnerRole.PARTNER_2
    uid == userUids.getOrNull(0) -> PartnerRole.PARTNER_1
    uid == userUids.getOrNull(1) -> PartnerRole.PARTNER_2
    else -> null
}

/** Whether any user is bound to the space; without members (demo, never synced) nobody else's data can be hit. */
val CoupleSpace.hasBoundMembers: Boolean
    get() = partner1Id != null || partner2Id != null || userUids.isNotEmpty()

/** Every UID bound to the space, explicit partner IDs and `userUids` together. */
val CoupleSpace.memberUids: Set<String>
    get() = (userUids + listOfNotNull(partner1Id, partner2Id)).filter { it.isNotBlank() }.toSet()

/** A swap needs both partners bound, or none (demo); with one member it would move their data into the empty slot. */
val CoupleSpace.canSwapPartners: Boolean
    get() = !hasBoundMembers || memberUids.size >= 2

/**
 * Whether [uid] can neither use nor join this space, so the server refuses every read and write: signed out
 * (`null`) while members exist, or a stranger to a space that already has two members.
 */
fun CoupleSpace.locksOut(uid: String?): Boolean = when {
    !hasBoundMembers -> false
    uid == null -> true
    else -> uid !in memberUids && memberUids.size >= 2
}

/**
 * The slot whose own data (profile photo) [uid] may change: the bound slot, or [fallbackRole] only in a space
 * without members. `null` means "not known yet" and editing has to wait.
 */
fun CoupleSpace.editableRole(uid: String?, fallbackRole: PartnerRole?): PartnerRole? =
    roleOf(uid) ?: fallbackRole.takeIf { !hasBoundMembers }

/**
 * Whether [uid] is partner 1, for display only. Explicit partner IDs decide; `userUids` order and the stored
 * [fallbackRole] are only fallbacks.
 */
fun CoupleSpace?.isPartner1(uid: String?, fallbackRole: PartnerRole): Boolean =
    (this?.roleOf(uid) ?: fallbackRole) == PartnerRole.PARTNER_1

fun CoupleSpace.nameOf(role: PartnerRole): String = if (role == PartnerRole.PARTNER_1) partner1Name else partner2Name

fun CoupleSpace.photoOf(role: PartnerRole): String? = if (role == PartnerRole.PARTNER_1) partner1PhotoUrl else partner2PhotoUrl

fun CoupleSpace.withPhoto(role: PartnerRole, url: String?): CoupleSpace =
    if (role == PartnerRole.PARTNER_1) copy(partner1PhotoUrl = url) else copy(partner2PhotoUrl = url)

fun Memory.withPhoto(role: PartnerRole, url: String?): Memory =
    if (role == PartnerRole.PARTNER_1) copy(partner1ImageUrl = url) else copy(partner2ImageUrl = url)
