package com.aistudio.couplebubble.qxztrw.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteOrganizerTest {

    private fun ids(): () -> String {
        var next = 0
        return { "id${++next}" }
    }

    @Test
    fun visibleNotesPutsPinnedFirstThenMostRecentlyChanged() {
        val notes = listOf(
            SharedNote(id = "old", updatedAt = 1),
            SharedNote(id = "pinnedOld", pinned = true, updatedAt = 2),
            SharedNote(id = "new", updatedAt = 5),
            SharedNote(id = "pinnedNew", pinned = true, updatedAt = 4)
        )

        val ordered = NoteOrganizer.visibleNotes(notes, filter = null).map { it.id }

        assertEquals(listOf("pinnedNew", "pinnedOld", "new", "old"), ordered)
    }

    @Test
    fun visibleNotesFiltersByCategory() {
        val notes = listOf(
            SharedNote(id = "shopping", labelId = DefaultNoteLabelIds.SHOPPING),
            SharedNote(id = "ideas", labelId = DefaultNoteLabelIds.IDEAS),
            SharedNote(id = "plain")
        )

        assertEquals(listOf("ideas"), NoteOrganizer.visibleNotes(notes, DefaultNoteLabelIds.IDEAS).map { it.id })
        assertEquals(3, NoteOrganizer.visibleNotes(notes, null).size)
        assertTrue(NoteOrganizer.visibleNotes(notes, DefaultNoteLabelIds.BUCKET_LIST).isEmpty())
    }

    @Test
    fun orderedItemsKeepsOpenItemsInPositionOrderAndCheckedOnesLast() {
        val items = listOf(
            NoteItem(id = "c", checked = true, position = 1),
            NoteItem(id = "b", position = 3),
            NoteItem(id = "a", position = 2),
            NoteItem(id = "d", checked = true, position = 0)
        )

        assertEquals(listOf("a", "b", "d", "c"), NoteOrganizer.orderedItems(items).map { it.id })
    }

    @Test
    fun progressCountsCheckedAndAllItems() {
        val note = SharedNote(
            items = listOf(
                NoteItem(id = "1", checked = true),
                NoteItem(id = "2"),
                NoteItem(id = "3", checked = true)
            )
        )

        assertEquals(2 to 3, NoteOrganizer.progress(note))
        assertEquals(0 to 0, NoteOrganizer.progress(SharedNote()))
    }

    @Test
    fun nextPositionFollowsTheHighestPosition() {
        assertEquals(1L, NoteOrganizer.nextPosition(emptyList()))
        assertEquals(8L, NoteOrganizer.nextPosition(listOf(NoteItem(position = 7), NoteItem(position = 2))))
    }

    @Test
    fun textToItemsDropsBlankLinesAndBullets() {
        val body = "- Milch\n\n  * Brot  \n• Käse\n[ ] Äpfel\n[x] Kaffee\n-\nTomaten"

        val items = NoteOrganizer.textToItems(body, createdBy = "uid_alex", startPosition = 5, newId = ids())

        assertEquals(listOf("Milch", "Brot", "Käse", "Äpfel", "Kaffee", "Tomaten"), items.map { it.text })
        assertEquals(listOf(false, false, false, false, true, false), items.map { it.checked })
        assertEquals(listOf(5L, 6L, 7L, 8L, 9L, 10L), items.map { it.position })
        assertTrue(items.all { it.createdBy == "uid_alex" })
        assertEquals(items.size, items.map { it.id }.toSet().size)
    }

    @Test
    fun textToItemsOfBlankTextIsEmpty() {
        assertTrue(NoteOrganizer.textToItems(" \n\n ", createdBy = null, newId = ids()).isEmpty())
    }

    @Test
    fun itemsToTextListsOpenItemsFirstAndKeepsCheckedPrefix() {
        val items = listOf(
            NoteItem(id = "1", text = "Kaffee", checked = true, position = 1),
            NoteItem(id = "2", text = "Milch", position = 2),
            NoteItem(id = "3", text = "  ", position = 3),
            NoteItem(id = "4", text = "Brot", position = 4)
        )

        assertEquals("Milch\nBrot\n[x] Kaffee", NoteOrganizer.itemsToText(items))
    }

    @Test
    fun textAndChecklistConversionRoundTripsTheItemTextsLosslesslyIncludingCheckedState() {
        val originalText = "Milch\nBrot\n[x] Kaffee"
        val items = NoteOrganizer.textToItems(originalText, createdBy = null, newId = ids())
        assertEquals(listOf(false, false, true), items.map { it.checked })
        assertEquals(listOf("Milch", "Brot", "Kaffee"), items.map { it.text })

        val roundTripText = NoteOrganizer.itemsToText(items)
        assertEquals(originalText, roundTripText)
    }

    @Test
    fun insertAfterPositionsComputesNewPositionAndShiftsSubsequentItems() {
        val openItems = listOf(
            NoteItem(id = "a", position = 1),
            NoteItem(id = "b", position = 2),
            NoteItem(id = "c", position = 3)
        )

        val (posAfterA, shiftedAfterA) = NoteOrganizer.insertAfterPositions(openItems, "a")
        assertEquals(2L, posAfterA)
        assertEquals(mapOf("b" to 3L, "c" to 4L), shiftedAfterA)

        val (posAfterC, shiftedAfterC) = NoteOrganizer.insertAfterPositions(openItems, "c")
        assertEquals(4L, posAfterC)
        assertTrue(shiftedAfterC.isEmpty())

        val (fallbackPos, shiftedFallback) = NoteOrganizer.insertAfterPositions(openItems, "unknown")
        assertEquals(4L, fallbackPos)
        assertTrue(shiftedFallback.isEmpty())
    }

    @Test
    fun isEmptyIgnoresBlankContent() {
        assertTrue(SharedNote(title = " ", body = "\n", items = listOf(NoteItem(text = " "))).isEmpty)
        assertFalse(SharedNote(title = "Urlaub").isEmpty)
        assertFalse(SharedNote(body = "Ideen").isEmpty)
        assertFalse(SharedNote(items = listOf(NoteItem(text = "Sonnencreme"))).isEmpty)
    }

    @Test
    fun authorColorsMapBoundPartnersToTheirAccentColor() {
        val space = CoupleSpace(
            partner1Id = "uid_alex",
            partner2Id = "uid_sam",
            partner1ColorHex = "#E65D2E",
            partner2ColorHex = "#4ECDC4"
        )

        val colors = NoteOrganizer.authorColors(space)

        assertEquals(mapOf("uid_alex" to "#E65D2E", "uid_sam" to "#4ECDC4"), colors)
        assertEquals(null, colors["uid_stranger"])
    }

    @Test
    fun authorColorsFollowARoleSwap() {
        val swapped = CoupleSpace(
            partner1Id = "uid_sam",
            partner2Id = "uid_alex",
            partner1ColorHex = "#4ECDC4",
            partner2ColorHex = "#E65D2E"
        )

        assertEquals("#E65D2E", NoteOrganizer.authorColors(swapped)["uid_alex"])
    }

    @Test
    fun authorColorsSkipUnboundSlotsAndMissingSpace() {
        assertEquals(mapOf("uid_alex" to "#E65D2E"), NoteOrganizer.authorColors(CoupleSpace(partner1Id = "uid_alex", partner1ColorHex = "#E65D2E")))
        assertTrue(NoteOrganizer.authorColors(null).isEmpty())
    }

    @Test
    fun authorNamesMapBoundPartnersToTheirName() {
        val space = CoupleSpace(
            partner1Id = "uid_alex",
            partner2Id = "uid_sam",
            partner1Name = "Alex",
            partner2Name = "Sam"
        )

        val names = NoteOrganizer.authorNames(space)

        assertEquals(mapOf("uid_alex" to "Alex", "uid_sam" to "Sam"), names)
        assertEquals(null, names["uid_stranger"])
    }

    @Test
    fun authorNamesFollowARoleSwap() {
        val swapped = CoupleSpace(
            partner1Id = "uid_sam",
            partner2Id = "uid_alex",
            partner1Name = "Sam",
            partner2Name = "Alex"
        )

        assertEquals("Alex", NoteOrganizer.authorNames(swapped)["uid_alex"])
    }

    @Test
    fun authorNamesSkipMissingSpace() {
        assertTrue(NoteOrganizer.authorNames(null).isEmpty())
    }

    @Test
    fun labelNamesAreTrimmedCollapsedAndLimited() {
        assertEquals("Lissabon 2027", NoteOrganizer.normalizeLabelName("  Lissabon \t  2027 "))
        assertEquals("", NoteOrganizer.normalizeLabelName("   "))
        assertEquals(NoteOrganizer.MAX_LABEL_NAME_LENGTH, NoteOrganizer.normalizeLabelName("x".repeat(50)).length)
    }

    @Test
    fun takenLabelNamesIgnoreCaseSpacesAndTheLabelItself() {
        val labels = listOf(NoteLabel("a", "Einkauf", 1), NoteLabel("b", "Bucket List", 2))

        assertTrue(NoteOrganizer.isLabelNameTaken(labels, " einkauf "))
        assertTrue(NoteOrganizer.isLabelNameTaken(labels, "bucket   list"))
        assertFalse(NoteOrganizer.isLabelNameTaken(labels, "EINKAUF", exceptId = "a"))
        assertFalse(NoteOrganizer.isLabelNameTaken(labels, "Ideen"))
    }

    @Test
    fun labelsSortByPositionThenName() {
        val labels = listOf(NoteLabel("c", "Zelten", 2), NoteLabel("b", "ausgehen", 2), NoteLabel("a", "Kino", 1))

        assertEquals(listOf("a", "b", "c"), NoteOrganizer.sortedLabels(labels).map { it.id })
        assertEquals(3L, NoteOrganizer.nextLabelPosition(labels))
        assertEquals(1L, NoteOrganizer.nextLabelPosition(emptyList()))
    }

    @Test
    fun labelUsageCountsLabeledNotesOnly() {
        val notes = listOf(
            SharedNote(id = "1", labelId = DefaultNoteLabelIds.IDEAS),
            SharedNote(id = "2", labelId = DefaultNoteLabelIds.IDEAS),
            SharedNote(id = "3", labelId = DefaultNoteLabelIds.SHOPPING),
            SharedNote(id = "4")
        )

        assertEquals(mapOf(DefaultNoteLabelIds.IDEAS to 2, DefaultNoteLabelIds.SHOPPING to 1), NoteOrganizer.labelUsage(notes))
    }

    private val stored = listOf(NoteLabel("a", "Einkauf", 1), NoteLabel("b", "Ideen", 2))
    private fun untouched() = stored.map { DraftLabel(it.id, it.name, it.name) }

    @Test
    fun labelChangesOnlyWriteWhatChanged() {
        val drafts = listOf(
            DraftLabel("a", " Einkauf ", "Einkauf"),
            DraftLabel("b", "Ideen", "Ideen", isDeleted = true),
            DraftLabel("n", "Filme", originalName = null),
            DraftLabel("gone", "Weg", originalName = null, isDeleted = true)
        )

        val changes = NoteOrganizer.labelChanges(stored, drafts, writeAll = false)

        assertEquals(listOf(NoteLabel("n", "Filme", 3)), changes.upserts)
        assertEquals(setOf("b"), changes.deletedIds)
    }

    @Test
    fun labelChangesWriteAllKeptLabelsWhileDefaultsApply() {
        val changes = NoteOrganizer.labelChanges(stored, untouched(), writeAll = true)

        assertEquals(stored, changes.upserts)
        assertTrue(changes.deletedIds.isEmpty())
    }

    @Test
    fun typedNameCountsAsAChange() {
        assertFalse(NoteOrganizer.hasLabelChanges(untouched(), "  "))
        assertTrue(NoteOrganizer.hasLabelChanges(untouched(), "Filme"))
        assertFalse(NoteOrganizer.hasLabelChanges(untouched().map { it.copy(name = " ${it.name}") }, ""))
    }

    @Test
    fun draftErrorsIgnoreDeletedRows() {
        val drafts = listOf(
            DraftLabel("a", "ideen", "Einkauf"),
            DraftLabel("b", "Ideen", "Ideen", isDeleted = true),
            DraftLabel("c", " ", originalName = null)
        )

        assertEquals(mapOf("c" to LabelNameError.EMPTY), NoteOrganizer.draftErrors(drafts))
        assertEquals(LabelNameError.TAKEN, NoteOrganizer.newLabelError(drafts, "IDEEN"))
        assertEquals(null, NoteOrganizer.newLabelError(drafts, ""))
    }

    @Test
    fun mergeKeepsLocalEditsAndTakesTheRest() {
        val drafts = listOf(DraftLabel("a", "Supermarkt", "Einkauf"), DraftLabel("b", "Ideen", "Ideen"))
        val remote = listOf(NoteLabel("a", "Laden", 1), NoteLabel("c", "Reisen", 3))

        val merged = NoteOrganizer.mergeRemoteLabels(drafts, remote)

        assertEquals(listOf(DraftLabel("a", "Supermarkt", "Einkauf"), DraftLabel("c", "Reisen", "Reisen")), merged)
    }
}
