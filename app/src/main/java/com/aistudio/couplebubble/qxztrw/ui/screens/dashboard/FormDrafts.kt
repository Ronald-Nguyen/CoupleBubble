package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import java.time.LocalDate

/*
 * Snapshots of the dashboard dialogs' inputs. A dialog keeps the snapshot it opened with and compares it to the
 * current input: without changes Back / a tap outside closes at once, otherwise DiscardChangesDialog asks first.
 * Texts are compared trimmed, since saving trims them as well.
 */

/** Input of [MemoryFormDialog]; a picked photo is a change even if no URL changed yet. */
internal data class MemoryFormDraft(
    val title: String,
    val date: LocalDate,
    val note: String,
    val countsAsMilestone: Boolean,
    val photo1: String?,
    val photo2: String?,
    val pickedPhoto1: Boolean = false,
    val pickedPhoto2: Boolean = false
)

internal fun MemoryFormDraft.hasChangesFrom(initial: MemoryFormDraft): Boolean =
    copy(title = title.trim(), note = note.trim()) != initial.copy(title = initial.title.trim(), note = initial.note.trim())

/** Input of [ReunionDialog]. */
internal data class ReunionDraft(val startDate: LocalDate, val endDate: LocalDate, val shareLocation: Boolean)

internal fun ReunionDraft.hasChangesFrom(initial: ReunionDraft): Boolean = this != initial

/** Input of [AddCustomMilestoneDialog]. */
internal data class CustomMilestoneDraft(val title: String, val date: LocalDate, val recurringYearly: Boolean)

internal fun CustomMilestoneDraft.hasChangesFrom(initial: CustomMilestoneDraft): Boolean =
    copy(title = title.trim()) != initial.copy(title = initial.title.trim())

/** Input of [EditNamesDialog]; [swapCount] counts confirmed role swaps, which already went to the server. */
internal data class EditNamesFields(
    val name1: String,
    val name2: String,
    val color1: String,
    val color2: String,
    val swapCount: Int = 0
)

/** A confirmed swap is no unsaved change, so [initial] is crossed once per swap before comparing. */
internal fun EditNamesFields.hasChangesFrom(initial: EditNamesFields): Boolean {
    val baseline = if (swapCount % 2 == 0) initial else {
        initial.copy(name1 = initial.name2, name2 = initial.name1, color1 = initial.color2, color2 = initial.color1)
    }
    return name1.trim() != baseline.name1.trim() || name2.trim() != baseline.name2.trim() ||
        !color1.equals(baseline.color1, ignoreCase = true) || !color2.equals(baseline.color2, ignoreCase = true)
}
