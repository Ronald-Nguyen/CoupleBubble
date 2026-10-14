package com.aistudio.couplebubble.qxztrw.data.remote.mapper

import com.aistudio.couplebubble.qxztrw.data.remote.FirestoreSchema
import com.aistudio.couplebubble.qxztrw.domain.model.NoteItem
import com.aistudio.couplebubble.qxztrw.domain.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.domain.model.NoteType
import com.aistudio.couplebubble.qxztrw.domain.model.SharedNote

/** Reads and writes `spaces/{id}/notes/{noteId}` and the shared label document. */
internal object NoteMapper {

    fun fromFields(id: String, fields: Fields): SharedNote? = try {
        val items = fields.entries(FirestoreSchema.Note.ITEMS).map { (itemId, item) ->
            NoteItem(
                id = itemId,
                text = item.string(FirestoreSchema.NoteItem.TEXT).orEmpty(),
                checked = item.bool(FirestoreSchema.NoteItem.CHECKED) ?: false,
                createdBy = item.string(FirestoreSchema.NoteItem.CREATED_BY),
                position = item.long(FirestoreSchema.NoteItem.POSITION) ?: 0L
            )
        }
        SharedNote(
            id = id,
            title = fields.string(FirestoreSchema.Note.TITLE).orEmpty(),
            body = fields.string(FirestoreSchema.Note.BODY).orEmpty(),
            type = NoteType.entries.firstOrNull { it.name == fields.string(FirestoreSchema.Note.TYPE) } ?: NoteType.TEXT,
            labelId = fields.string(FirestoreSchema.Note.LABEL_ID),
            pinned = fields.bool(FirestoreSchema.Note.PINNED) ?: false,
            items = items,
            createdBy = fields.string(FirestoreSchema.Note.CREATED_BY),
            updatedBy = fields.string(FirestoreSchema.Note.UPDATED_BY),
            createdAt = fields.long(FirestoreSchema.Note.CREATED_AT) ?: 0L,
            updatedAt = fields.long(FirestoreSchema.Note.UPDATED_AT) ?: 0L
        )
    } catch (_: Exception) {
        null
    }

    fun toFields(note: SharedNote): Map<String, Any?> = mapOf(
        FirestoreSchema.Note.TITLE to note.title,
        FirestoreSchema.Note.BODY to note.body,
        FirestoreSchema.Note.TYPE to note.type.name,
        FirestoreSchema.Note.LABEL_ID to note.labelId,
        FirestoreSchema.Note.PINNED to note.pinned,
        FirestoreSchema.Note.ITEMS to itemsMap(note.items),
        FirestoreSchema.Note.CREATED_BY to note.createdBy,
        FirestoreSchema.Note.UPDATED_BY to note.updatedBy,
        FirestoreSchema.Note.CREATED_AT to note.createdAt,
        FirestoreSchema.Note.UPDATED_AT to note.updatedAt
    )

    fun itemFields(item: NoteItem): Map<String, Any?> = mapOf(
        FirestoreSchema.NoteItem.TEXT to item.text,
        FirestoreSchema.NoteItem.CHECKED to item.checked,
        FirestoreSchema.NoteItem.CREATED_BY to item.createdBy,
        FirestoreSchema.NoteItem.POSITION to item.position
    )

    fun itemsMap(items: List<NoteItem>): Map<String, Map<String, Any?>> = items.associate { it.id to itemFields(it) }

    /** The stored labels, or `null` while nobody has saved any and the defaults apply. */
    fun labelsFromFields(fields: Fields?): List<NoteLabel>? = fields?.takeIf { it[FirestoreSchema.NoteLabels.LABELS] is Map<*, *> }?.let {
        it.entries(FirestoreSchema.NoteLabels.LABELS).mapNotNull { (labelId, label) ->
            NoteLabel(
                id = labelId,
                name = label.string(FirestoreSchema.NoteLabels.NAME) ?: return@mapNotNull null,
                position = label.long(FirestoreSchema.NoteLabels.POSITION) ?: 0L
            )
        }
    }

    fun labelFields(label: NoteLabel): Map<String, Any> = mapOf(
        FirestoreSchema.NoteLabels.NAME to label.name,
        FirestoreSchema.NoteLabels.POSITION to label.position
    )
}
