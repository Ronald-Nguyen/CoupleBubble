package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormDraftsTest {

    private val date = LocalDate.of(2026, 5, 1)

    @Test
    fun memoryDraftUnchangedOrOnlyWhitespaceHasNoChanges() {
        val initial = MemoryFormDraft("Picknick", date, "", false, "https://a", null)
        assertFalse(initial.hasChangesFrom(initial))
        assertFalse(initial.copy(title = "Picknick  ", note = " ").hasChangesFrom(initial))
    }

    @Test
    fun memoryDraftDetectsEveryField() {
        val initial = MemoryFormDraft("", date, "", false, "https://a", null)
        assertTrue(initial.copy(title = "See").hasChangesFrom(initial))
        assertTrue(initial.copy(date = date.plusDays(1)).hasChangesFrom(initial))
        assertTrue(initial.copy(note = "schön").hasChangesFrom(initial))
        assertTrue(initial.copy(countsAsMilestone = true).hasChangesFrom(initial))
        assertTrue(initial.copy(photo1 = null).hasChangesFrom(initial))
        assertTrue(initial.copy(pickedPhoto2 = true).hasChangesFrom(initial))
    }

    @Test
    fun reunionDraftDetectsDatesAndSharing() {
        val initial = ReunionDraft(date, date.plusDays(2), false)
        assertFalse(initial.copy().hasChangesFrom(initial))
        assertTrue(initial.copy(endDate = date.plusDays(3)).hasChangesFrom(initial))
        assertTrue(initial.copy(shareLocation = true).hasChangesFrom(initial))
    }

    @Test
    fun customMilestoneDraftIgnoresBlankTitle() {
        val initial = CustomMilestoneDraft("", date, false)
        assertFalse(initial.copy(title = "   ").hasChangesFrom(initial))
        assertTrue(initial.copy(title = "Umzug").hasChangesFrom(initial))
        assertTrue(initial.copy(recurringYearly = true).hasChangesFrom(initial))
    }

    @Test
    fun editNamesConfirmedSwapIsNoChange() {
        val initial = EditNamesFields("Mia", "Leo", "#E65D2E", "#5B8DB8")
        val swapped = EditNamesFields("Leo", "Mia", "#5B8DB8", "#E65D2E", swapCount = 1)
        assertFalse(swapped.hasChangesFrom(initial))
        assertFalse(swapped.copy(name1 = swapped.name2, name2 = swapped.name1, color1 = swapped.color2, color2 = swapped.color1, swapCount = 2).hasChangesFrom(initial))
        assertTrue(swapped.copy(name1 = "Leonie").hasChangesFrom(initial))
    }

    @Test
    fun editNamesDetectsRenameAndColor() {
        val initial = EditNamesFields("Mia", "Leo", "#E65D2E", "#5B8DB8")
        assertFalse(initial.copy(name1 = "Mia ", color1 = "#e65d2e").hasChangesFrom(initial))
        assertTrue(initial.copy(name2 = "Leon").hasChangesFrom(initial))
        assertTrue(initial.copy(color2 = "#0F2137").hasChangesFrom(initial))
    }
}
