package com.aistudio.couplebubble.qxztrw.data.remote

/** Collection and field names as stored in Firestore. Changing a value breaks existing data. */
internal object FirestoreSchema {
    const val SPACES = "spaces"
    const val USERS = "users"
    const val MEMORIES = "memories"
    const val MILESTONES = "milestones"
    const val NOTES = "notes"
    const val SETTINGS = "settings"
    const val NOTE_LABELS_DOC = "noteLabels"
    const val REUNION = "reunion"
    const val REUNION_DOC = "current"

    /** Fields of `spaces/{id}`. Partner 1/2 values are mirrored into their legacy A/B aliases. */
    object Space {
        const val ID = "id"
        const val PAIRING_CODE = "pairingCode"
        const val PARTNER_1_NAME = "partnerAName"
        const val PARTNER_2_NAME = "partnerBName"
        const val PARTNER_1_NAME_ALIAS = "partner1Name"
        const val PARTNER_2_NAME_ALIAS = "partner2Name"
        const val PARTNER_1_PHOTO = "partner1PhotoUrl"
        const val PARTNER_2_PHOTO = "partner2PhotoUrl"
        const val PARTNER_1_PHOTO_ALIAS = "partnerAPhotoUrl"
        const val PARTNER_2_PHOTO_ALIAS = "partnerBPhotoUrl"
        const val PARTNER_1_COLOR = "partner1ColorHex"
        const val PARTNER_2_COLOR = "partner2ColorHex"
        const val PARTNER_1_COLOR_ALIAS = "partnerAColorHex"
        const val PARTNER_2_COLOR_ALIAS = "partnerBColorHex"
        const val PARTNER_1_ID = "partner1Id"
        const val PARTNER_2_ID = "partner2Id"
        const val USER_UIDS = "userUids"
        const val MEMBERS = "members"
        const val ANNIVERSARY_YEAR = "anniversaryYear"
        const val ANNIVERSARY_MONTH = "anniversaryMonth"
        const val ANNIVERSARY_DAY = "anniversaryDay"
        const val ANNIVERSARY_EPOCH_MILLIS = "anniversaryEpochMillis"
        const val IS_SETUP_COMPLETE = "isSetupComplete"
        const val IS_ACTIVE = "isActive"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val PAIRED_AT = "pairedAt"
        const val DISCONNECTED_AT = "disconnectedAt"
    }

    /** Fields of `spaces/{id}/memories/{memoryId}`. */
    object Memory {
        const val ID = "id"
        const val TITLE = "title"
        const val DATE = "date"
        const val NOTE = "note"
        const val LEGACY_IMAGE_URL = "imageUrl"
        const val PARTNER_1_IMAGE = "partnerAImageUrl"
        const val PARTNER_2_IMAGE = "partnerBImageUrl"
        const val CREATED_AT = "createdAt"
        const val CREATED_BY = "createdBy"
        const val COUNTS_AS_MILESTONE = "countsAsMilestone"
        const val UPDATED_AT = "updatedAt"
    }

    /** Fields of `users/{uid}`. */
    object User {
        const val UID = "uid"
        const val EMAIL = "email"
        const val DISPLAY_NAME = "displayName"
        const val COUPLE_ID = "coupleId"
        const val UPDATED_AT = "updatedAt"
    }

    /** Fields of `spaces/{id}/milestones/{milestoneId}`. */
    object Milestone {
        const val TITLE = "title"
        const val DATE = "date"
        const val RECURRING_YEARLY = "recurringYearly"
        const val ENABLED = "enabled"
        const val CREATED_AT = "createdAt"
    }

    /** Fields of `spaces/{id}/notes/{noteId}`; checklist items live in the map [ITEMS] keyed by item id. */
    object Note {
        const val TITLE = "title"
        const val BODY = "body"
        const val TYPE = "type"

        // Stored as "category" since labels used to be fixed categories; their IDs stayed the same
        const val LABEL_ID = "category"
        const val PINNED = "pinned"
        const val ITEMS = "items"
        const val CREATED_BY = "createdBy"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }

    /** Fields of one entry in [Note.ITEMS]. */
    object NoteItem {
        const val TEXT = "text"
        const val CHECKED = "checked"
        const val CREATED_BY = "createdBy"
        const val POSITION = "position"
    }

    /** `spaces/{id}/settings/noteLabels`: the map [LABELS] keyed by label id. */
    object NoteLabels {
        const val LABELS = "labels"
        const val NAME = "name"
        const val POSITION = "position"
    }

    /** `spaces/{id}/reunion/current`: shared dates plus one [ReunionPlace] entry per partner UID in [PLACES]. */
    object Reunion {
        const val START_DATE = "startDate"
        const val END_DATE = "endDate"
        const val PLANNED_AT = "plannedAt"
        const val PLACES = "places"
        const val UPDATED_AT = "updatedAt"
    }

    /** Fields of one entry in [Reunion.PLACES]; coordinates are stored rounded to one decimal place. */
    object ReunionPlace {
        const val SHARES_LOCATION = "sharesLocation"
        const val LATITUDE = "lat"
        const val LONGITUDE = "lng"
        const val LOCATED_AT = "locatedAt"
    }
}
