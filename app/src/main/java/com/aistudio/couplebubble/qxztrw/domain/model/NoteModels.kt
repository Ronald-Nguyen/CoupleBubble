package com.aistudio.couplebubble.qxztrw.domain.model

enum class NoteType { TEXT, CHECKLIST }

/** A label both partners can rename, delete or add; notes reference it by [id]. */
data class NoteLabel(
    val id: String = "",
    val name: String = "",
    val position: Long = 0L
)

/**
 * One row of the label editor. Nothing is written until the user saves; [originalName] is `null` for a label
 * added in this editing session.
 */
data class DraftLabel(
    val id: String,
    val name: String,
    val originalName: String?,
    val isDeleted: Boolean = false
) {
    val isNew: Boolean get() = originalName == null
}

enum class LabelNameError { EMPTY, TAKEN }

/** What saving the label editor writes: labels to create or rename, and labels to remove. */
data class LabelChanges(val upserts: List<NoteLabel>, val deletedIds: Set<String>) {
    val isEmpty: Boolean get() = upserts.isEmpty() && deletedIds.isEmpty()
}

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
    val updatedBy: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val isEmpty: Boolean get() = title.isBlank() && body.isBlank() && items.none { it.text.isNotBlank() }
    val checkedCount: Int get() = items.count { it.checked }
}

object NoteOrganizer {

    private val BULLET_PREFIX = Regex("""^([-*•]|\[ ?])\s*""")
    private val CHECKED_PREFIX = Regex("""^\[[xX]]\s*""")

    val MAX_LABEL_NAME_LENGTH = InputField.LABEL_NAME.maxLength

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

    /**
     * Positions for [openItems] in the order of [orderedIds]; items the list doesn't know (added meanwhile)
     * keep their relative order at the end. Only changed positions are returned.
     */
    fun reorderedPositions(openItems: List<NoteItem>, orderedIds: List<String>): Map<String, Long> {
        val byId = openItems.associateBy { it.id }
        val known = orderedIds.distinct().mapNotNull { byId[it] }
        val reordered = known + openItems.filterNot { it in known }
        val start = openItems.minOfOrNull { it.position } ?: 0L
        return reordered.withIndex()
            .filter { (index, item) -> item.position != start + index }
            .associate { (index, item) -> item.id to start + index }
    }

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

    /** One line per item (open ones first); checked items keep an `[x]` prefix. */
    fun itemsToText(items: List<NoteItem>): String =
        orderedItems(items)
            .filter { it.text.isNotBlank() }
            .map { item ->
                val text = item.text.trim()
                if (item.checked) "[x] $text" else text
            }
            .joinToString(separator = "\n")

    /**
     * Positions for inserting a new item directly after [afterItemId] in [openItems].
     * Returns the new item's position and any changed positions for existing items.
     */
    fun insertAfterPositions(
        openItems: List<NoteItem>,
        afterItemId: String
    ): Pair<Long, Map<String, Long>> {
        val targetIndex = openItems.indexOfFirst { it.id == afterItemId }
        if (targetIndex == -1) {
            return nextPosition(openItems) to emptyMap()
        }
        val start = openItems.minOfOrNull { it.position } ?: 1L
        val newPos = start + targetIndex + 1L
        val shifted = openItems.drop(targetIndex + 1).mapIndexedNotNull { index, item ->
            val expectedPos = newPos + 1L + index
            if (item.position != expectedPos) item.id to expectedPos else null
        }.toMap()
        return newPos to shifted
    }

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

    /** Name problems of the rows that will be kept; deleted rows neither count nor block duplicates. */
    fun draftErrors(drafts: List<DraftLabel>): Map<String, LabelNameError> {
        val kept = drafts.filterNot { it.isDeleted }
        return kept.mapNotNull { draft ->
            val normalized = normalizeLabelName(draft.name).lowercase()
            val error = when {
                normalized.isEmpty() -> LabelNameError.EMPTY
                kept.any { it.id != draft.id && normalizeLabelName(it.name).lowercase() == normalized } -> LabelNameError.TAKEN
                else -> null
            }
            error?.let { draft.id to it }
        }.toMap()
    }

    /** Problem of the text in the "new label" field; an empty field is simply nothing to add. */
    fun newLabelError(drafts: List<DraftLabel>, name: String): LabelNameError? {
        val normalized = normalizeLabelName(name).lowercase()
        if (normalized.isEmpty()) return null
        val taken = drafts.any { !it.isDeleted && normalizeLabelName(it.name).lowercase() == normalized }
        return if (taken) LabelNameError.TAKEN else null
    }

    /** Whether saving would change anything, counting a typed but not yet added label. */
    fun hasLabelChanges(drafts: List<DraftLabel>, newName: String): Boolean =
        normalizeLabelName(newName).isNotEmpty() || drafts.any { draft ->
            if (draft.isNew) !draft.isDeleted
            else draft.isDeleted || normalizeLabelName(draft.name) != draft.originalName
        }

    /**
     * Takes the partner's label changes into an open editor: untouched rows follow remote renames, rows of labels
     * the partner deleted disappear unless edited here, and labels the partner added show up as new rows.
     */
    fun mergeRemoteLabels(drafts: List<DraftLabel>, remote: List<NoteLabel>): List<DraftLabel> {
        val remoteById = remote.associateBy { it.id }
        val merged = drafts.mapNotNull { draft ->
            if (draft.isNew) return@mapNotNull draft
            val current = remoteById[draft.id]
            val untouched = !draft.isDeleted && draft.name == draft.originalName
            when {
                current == null -> draft.takeUnless { untouched }
                untouched -> draft.copy(name = current.name, originalName = current.name)
                else -> draft
            }
        }
        val known = drafts.map { it.id }.toSet()
        return merged + remote.filter { it.id !in known }.map { DraftLabel(it.id, it.name, it.name) }
    }

    /**
     * The writes for saving [drafts] against the stored [current] labels. With [writeAll] (the space still shows
     * the defaults) every kept label is written, because from then on only stored labels count. Renames of labels
     * the partner deleted meanwhile are skipped.
     */
    fun labelChanges(current: List<NoteLabel>, drafts: List<DraftLabel>, writeAll: Boolean): LabelChanges {
        val currentById = current.associateBy { it.id }
        var nextPosition = nextLabelPosition(current)
        val upserts = drafts.filterNot { it.isDeleted }.mapNotNull { draft ->
            val name = normalizeLabelName(draft.name)
            if (draft.isNew) return@mapNotNull NoteLabel(draft.id, name, nextPosition++)
            val stored = currentById[draft.id] ?: return@mapNotNull null
            stored.copy(name = name).takeIf { writeAll || name != stored.name }
        }
        val deleted = drafts.filter { it.isDeleted && !it.isNew && it.id in currentById }.map { it.id }.toSet()
        return LabelChanges(upserts, deleted)
    }

    /** How many notes carry each label; labels without notes are left out. */
    fun labelUsage(notes: List<SharedNote>): Map<String, Int> =
        notes.mapNotNull { it.labelId }.groupingBy { it }.eachCount()

    /** Maps each bound partner's UID to their accent color, so author dots follow a role swap. */
    fun authorColors(space: CoupleSpace?): Map<String, String> {
        if (space == null) return emptyMap()
        return buildMap {
            // Same resolution as roleOf: explicit partner IDs first, userUids order only for unbound slots
            (space.partner1Id ?: space.userUids.getOrNull(0))?.let { put(it, space.partner1ColorHex) }
            (space.partner2Id ?: space.userUids.getOrNull(1))?.takeIf { it !in this }?.let { put(it, space.partner2ColorHex) }
        }
    }

    /** Maps each bound partner's UID to their display name, so names follow a role swap. */
    fun authorNames(space: CoupleSpace?): Map<String, String> {
        if (space == null) return emptyMap()
        return buildMap {
            (space.partner1Id ?: space.userUids.getOrNull(0))?.let { uid ->
                space.roleOf(uid)?.let { role -> put(uid, space.nameOf(role)) }
            }
            (space.partner2Id ?: space.userUids.getOrNull(1))?.let { uid ->
                if (uid !in this) {
                    space.roleOf(uid)?.let { role -> put(uid, space.nameOf(role)) }
                }
            }
        }
    }
}
