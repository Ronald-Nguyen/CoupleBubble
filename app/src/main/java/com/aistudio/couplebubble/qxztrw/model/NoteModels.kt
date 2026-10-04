package com.aistudio.couplebubble.qxztrw.model

enum class NoteType { TEXT, CHECKLIST }

/** A label both partners can rename, delete or add; notes reference it by [id]. */
data class NoteLabel(
    val id: String = "",
    val name: String = "",
    val position: Long = 0L
)

/**
 * IDs of the labels every space starts with. They match the former fixed categories, so notes that were
 * labeled before labels became editable keep their label.
 */
object DefaultNoteLabelIds {
    const val SHOPPING = "SHOPPING"
    const val BUCKET_LIST = "BUCKET_LIST"
    const val IDEAS = "IDEAS"
}

data class NoteItem(
    val id: String = "",
    val text: String = "",
    val checked: Boolean = false,
    val createdBy: String? = null,
    val position: Long = 0L
)

/** A text note or checklist shared by both partners (`spaces/{id}/notes`). */
data class SharedNote(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: NoteType = NoteType.TEXT,
    val labelId: String? = null,
    val pinned: Boolean = false,
    val items: List<NoteItem> = emptyList(),
    val createdBy: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val isEmpty: Boolean get() = title.isBlank() && body.isBlank() && items.none { it.text.isNotBlank() }
    val checkedCount: Int get() = items.count { it.checked }
}

object NoteOrganizer {

    private val BULLET_PREFIX = Regex("""^([-*•]|\[ ?])\s*""")
    private val CHECKED_PREFIX = Regex("""^\[[xX]]\s*""")

    const val MAX_LABEL_NAME_LENGTH = 30

    private val WHITESPACE = Regex("""\s+""")

    /** Notes carrying the label [filter] (all for `null`): pinned first, then most recently changed. */
    fun visibleNotes(notes: List<SharedNote>, filter: String?): List<SharedNote> =
        notes
            .filter { filter == null || it.labelId == filter }
            .sortedWith(compareByDescending<SharedNote> { it.pinned }.thenByDescending { it.updatedAt })

    /** Open items in their list order, followed by the checked ones. */
    fun orderedItems(items: List<NoteItem>): List<NoteItem> =
        items.sortedWith(compareBy<NoteItem> { it.checked }.thenBy { it.position })

    /** Checked items and all items of a checklist, e.g. 3 to 8 for "3 von 8 erledigt". */
    fun progress(note: SharedNote): Pair<Int, Int> = note.checkedCount to note.items.size

    fun nextPosition(items: List<NoteItem>): Long = (items.maxOfOrNull { it.position } ?: 0L) + 1L

    /**
     * Turns every non-blank line of [body] into an item. Leading bullets (`-`, `*`, `•`, `[ ]`) are dropped,
     * `[x]` marks the item as checked.
     */
    fun textToItems(
        body: String,
        createdBy: String?,
        startPosition: Long = 1L,
        newId: () -> String
    ): List<NoteItem> =
        body.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val checked = CHECKED_PREFIX.containsMatchIn(line)
                val text = line.replaceFirst(if (checked) CHECKED_PREFIX else BULLET_PREFIX, "").trim()
                if (text.isEmpty()) null else text to checked
            }
            .mapIndexed { index, (text, checked) ->
                NoteItem(
                    id = newId(),
                    text = text,
                    checked = checked,
                    createdBy = createdBy,
                    position = startPosition + index
                )
            }

    /** One line per item (open ones first); the check state is not carried over into plain text. */
    fun itemsToText(items: List<NoteItem>): String =
        orderedItems(items)
            .map { it.text.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n")

    fun sortedLabels(labels: List<NoteLabel>): List<NoteLabel> =
        labels.sortedWith(compareBy<NoteLabel> { it.position }.thenBy { it.name.lowercase() })

    /** Trimmed, inner whitespace collapsed and cut to [MAX_LABEL_NAME_LENGTH]. */
    fun normalizeLabelName(name: String): String =
        name.trim().replace(WHITESPACE, " ").take(MAX_LABEL_NAME_LENGTH).trim()

    /** Whether another label (not [exceptId]) already uses [name], ignoring case and surrounding spaces. */
    fun isLabelNameTaken(labels: List<NoteLabel>, name: String, exceptId: String? = null): Boolean {
        val normalized = normalizeLabelName(name).lowercase()
        return labels.any { it.id != exceptId && normalizeLabelName(it.name).lowercase() == normalized }
    }

    fun nextLabelPosition(labels: List<NoteLabel>): Long = (labels.maxOfOrNull { it.position } ?: 0L) + 1L

    /** How many notes carry each label; labels without notes are left out. */
    fun labelUsage(notes: List<SharedNote>): Map<String, Int> =
        notes.mapNotNull { it.labelId }.groupingBy { it }.eachCount()

    /** Maps each bound partner's UID to their accent color, so author dots follow a role swap. */
    fun authorColors(space: CoupleSpace?): Map<String, String> {
        if (space == null) return emptyMap()
        return buildMap {
            space.partner1Id?.let { put(it, space.partner1ColorHex) }
            space.partner2Id?.let { put(it, space.partner2ColorHex) }
        }
    }
}
