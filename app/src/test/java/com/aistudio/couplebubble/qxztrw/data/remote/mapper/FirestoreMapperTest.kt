package com.aistudio.couplebubble.qxztrw.data.remote.mapper

import com.aistudio.couplebubble.qxztrw.data.remote.FirestoreSchema
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.NoteItem
import com.aistudio.couplebubble.qxztrw.domain.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.domain.model.NoteType
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.SharedNote
import com.aistudio.couplebubble.qxztrw.domain.model.SpaceDefaults
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Runs the Firestore mappers on plain maps, the way `DocumentSnapshot.getData()` delivers them. */
class FirestoreMapperTest {

    @Test
    fun spaceReadsLegacyAliasesAndDerivesTheCodeFromTheId() {
        val space = requireNotNull(
            SpaceMapper.fromFields(
                "space_482913",
                mapOf(
                    "partnerAName" to "Mia",
                    "partner2Name" to "Leo",
                    "partnerBPhotoUrl" to "https://example.com/leo.jpg",
                    "anniversaryYear" to 2021L,
                    "userUids" to listOf("u1", 7, "u2")
                )
            )
        )

        assertEquals("Mia", space.partner1Name)
        assertEquals("Leo", space.partner2Name)
        assertEquals("https://example.com/leo.jpg", space.partner2PhotoUrl)
        assertEquals(2021, space.anniversaryYear)
        assertEquals(listOf("u1", "u2"), space.userUids)
        assertEquals("482913", space.pairingCode)
        assertEquals(SpaceDefaults.PARTNER_1_COLOR_HEX, space.partner1ColorHex)
        assertTrue("Changed names count as set up for spaces without the flag", space.isSetupComplete)
    }

    @Test
    fun disconnectedOrPlaceholderSpaces() {
        assertNull(SpaceMapper.fromFields("space_1", mapOf("isActive" to false)))

        val fresh = requireNotNull(SpaceMapper.fromFields("custom_id", emptyMap()))
        assertFalse(fresh.isSetupComplete)
        assertEquals("", fresh.pairingCode)
    }

    @Test
    fun wrongTypesReadAsMissing() {
        val space = requireNotNull(SpaceMapper.fromFields("s", mapOf("partnerAName" to 42, "isSetupComplete" to "yes")))

        assertEquals(SpaceDefaults.PARTNER_1_NAME, space.partner1Name)
    }

    @Test
    fun writesMirrorTheLegacyAliases() {
        assertEquals(
            mapOf("partnerAName" to "Mia", "partnerBName" to "Leo", "partner1Name" to "Mia", "partner2Name" to "Leo"),
            SpaceMapper.names("Mia", "Leo")
        )
        assertEquals(
            mapOf("partner2PhotoUrl" to "url", "partnerBPhotoUrl" to "url"),
            SpaceMapper.photo(PartnerRole.PARTNER_2, "url")
        )
    }

    @Test
    fun memoryUsesLegacyImageOnlyForOldSinglePhotoMoments() {
        val old = requireNotNull(
            MemoryMapper.fromFields("m1", mapOf("title" to "Paris", "date" to "2024-05-01", "imageUrl" to "https://x/a.jpg"))
        )
        assertEquals("https://x/a.jpg", old.partner1ImageUrl)
        assertEquals(LocalDate.of(2024, 5, 1), old.date)

        val mirrored = requireNotNull(
            MemoryMapper.fromFields("m2", mapOf("imageUrl" to "https://x/b.jpg", "partnerBImageUrl" to "https://x/b.jpg"))
        )
        assertNull(mirrored.partner1ImageUrl)
        assertEquals("https://x/b.jpg", mirrored.partner2ImageUrl)
    }

    @Test
    fun memoryWithoutDateFallsBackToToday() {
        val today = LocalDate.of(2026, 10, 13)

        assertEquals(today, MemoryMapper.fromFields("m", emptyMap(), today)?.date)
        assertNull("An unparsable date drops the moment", MemoryMapper.fromFields("m", mapOf("date" to "gestern")))
    }

    @Test
    fun momentMilestoneSwitchDefaultsToOff() {
        assertFalse(MemoryMapper.fromFields("m", mapOf("date" to "2024-08-12"))!!.countsAsMilestone)
        assertTrue(MemoryMapper.fromFields("m", mapOf("date" to "2024-08-12", "countsAsMilestone" to true))!!.countsAsMilestone)
    }

    @Test
    fun milestoneRoundTrip() {
        val milestone = CustomMilestone(id = "c1", title = "Hochzeit", date = LocalDate.of(2025, 6, 7), recurringYearly = true)

        val fields = MilestoneMapper.toFields(milestone, createdAt = 5L)

        assertEquals(milestone, MilestoneMapper.fromFields("c1", fields))
        assertEquals(5L, fields[FirestoreSchema.Milestone.CREATED_AT])
        assertNull(MilestoneMapper.fromFields("c2", mapOf("title" to "Ohne Datum")))
    }

    @Test
    fun noteRoundTripKeepsItemsAndTheLegacyLabelField() {
        val note = SharedNote(
            id = "n1",
            title = "Einkauf",
            type = NoteType.CHECKLIST,
            labelId = "shopping",
            pinned = true,
            items = listOf(NoteItem(id = "i1", text = "Milch", checked = true, createdBy = "u1", position = 3)),
            createdBy = "u1",
            updatedBy = "u2",
            createdAt = 1L,
            updatedAt = 2L
        )

        val fields = NoteMapper.toFields(note)

        assertEquals("shopping", fields["category"])
        assertEquals("u2", fields["updatedBy"])
        assertEquals(note, NoteMapper.fromFields("n1", fields))
    }

    @Test
    fun labelsAreNullUntilSomeoneSavedThem() {
        assertNull(NoteMapper.labelsFromFields(null))
        assertNull(NoteMapper.labelsFromFields(emptyMap()))
        assertEquals(
            listOf(NoteLabel("a", "Reisen", 2)),
            NoteMapper.labelsFromFields(
                mapOf("labels" to mapOf("a" to mapOf("name" to "Reisen", "position" to 2L), "b" to mapOf("position" to 1L)))
            )
        )
    }

    @Test
    fun boundPartnerIdsFallBackToUserUidsOnlyWithoutExplicitIds() {
        val space = FirestoreSchema.Space
        assertEquals("u1" to "u2", SpaceMapper.boundPartnerIds(mapOf(space.USER_UIDS to listOf("u1", "u2"))))
        assertEquals(
            null to "u2",
            SpaceMapper.boundPartnerIds(mapOf(space.PARTNER_2_ID to "u2", space.USER_UIDS to listOf("u1", "u2")))
        )
        assertEquals(null to null, SpaceMapper.boundPartnerIds(emptyMap()))
    }

    @Test
    fun joinIsRefusedOnlyForStrangersOfAFullSpace() {
        val space = FirestoreSchema.Space
        val full = mapOf(space.USER_UIDS to listOf("u1", "u2"))

        assertFalse(SpaceMapper.isJoinRefused(emptyMap(), "u3"))
        assertFalse(SpaceMapper.isJoinRefused(mapOf(space.USER_UIDS to listOf("u1")), "u3"))
        assertTrue(SpaceMapper.isJoinRefused(full, "u3"))
        assertTrue(SpaceMapper.isJoinRefused(full, null))
        assertFalse(SpaceMapper.isJoinRefused(full, "u2"))
        // Old spaces only know the legacy members array
        assertTrue(SpaceMapper.isJoinRefused(mapOf(space.MEMBERS to listOf("u1", "u2")), "u3"))
        // A bound partner 2 closes the space even before userUids lists them
        assertTrue(SpaceMapper.isJoinRefused(mapOf(space.PARTNER_2_ID to "u2"), "u3"))
        assertFalse(SpaceMapper.isJoinRefused(mapOf(space.PARTNER_1_ID to "u1", space.PARTNER_2_ID to "u2"), "u1"))
    }

    @Test
    fun joiningKeepsStoredValuesAndMapsLikeTheListener() {
        val stored = mapOf(
            FirestoreSchema.Space.PARTNER_1_NAME to "Mia",
            FirestoreSchema.Space.PARTNER_1_PHOTO_ALIAS to "https://old/photo.jpg",
            FirestoreSchema.Space.ANNIVERSARY_YEAR to 2021L,
            FirestoreSchema.Space.IS_ACTIVE to false
        )
        val joined = SpaceMapper.joined("space_482913", "482913", stored, now = 5L)
        val space = requireNotNull(SpaceMapper.fromFields("space_482913", stored + joined))

        assertEquals("Mia", space.partner1Name)
        assertEquals("https://old/photo.jpg", space.partner1PhotoUrl)
        assertEquals(2021, space.anniversaryYear)
        assertEquals("482913", space.pairingCode)
        // Changed names without the flag count as set up, like in fromFields
        assertTrue(space.isSetupComplete)
    }
}
