package com.aistudio.couplebubble.qxztrw.model

/** The role this device remembers for its user; only a fallback while the space has no explicit partner IDs. */
enum class PartnerRole(val storedValue: String) {
    PARTNER_1("1"),
    PARTNER_2("2");

    fun swapped(): PartnerRole = if (this == PARTNER_1) PARTNER_2 else PARTNER_1

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
 * Whether [uid] is partner 1. Explicit partner IDs decide; `userUids` order and the stored [fallbackRole]
 * are only fallbacks.
 */
fun CoupleSpace?.isPartner1(uid: String?, fallbackRole: PartnerRole): Boolean = when {
    this == null || uid == null -> fallbackRole == PartnerRole.PARTNER_1
    uid == partner1Id -> true
    uid == partner2Id -> false
    uid == userUids.getOrNull(0) -> true
    uid == userUids.getOrNull(1) -> false
    else -> fallbackRole == PartnerRole.PARTNER_1
}
