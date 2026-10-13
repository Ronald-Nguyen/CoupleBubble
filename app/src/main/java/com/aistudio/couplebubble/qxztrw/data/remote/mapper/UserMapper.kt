package com.aistudio.couplebubble.qxztrw.data.remote.mapper

import com.aistudio.couplebubble.qxztrw.data.remote.FirestoreSchema.User
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile

/** Reads and writes the user anchor `/users/{uid}`, which maps a UID to its space across reinstalls. */
internal object UserMapper {

    fun fromFields(uid: String, fields: Fields): UserProfile = UserProfile(
        uid = uid,
        email = fields.string(User.EMAIL),
        displayName = fields.string(User.DISPLAY_NAME),
        coupleId = fields.string(User.COUPLE_ID)
    )

    /** The anchor for a merge write; a blank [coupleId] is left out so the stored space is kept. */
    fun anchor(uid: String, email: String?, displayName: String?, coupleId: String?, now: Long): Map<String, Any?> =
        listOfNotNull(
            User.UID to uid,
            User.EMAIL to email,
            User.DISPLAY_NAME to displayName,
            User.UPDATED_AT to now,
            coupleId?.takeIf { it.isNotBlank() }?.let { User.COUPLE_ID to it }
        ).toMap()
}
