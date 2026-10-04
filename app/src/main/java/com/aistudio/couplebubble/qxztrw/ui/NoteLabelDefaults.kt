package com.aistudio.couplebubble.qxztrw.ui

import android.content.res.Resources
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.model.DefaultNoteLabelIds
import com.aistudio.couplebubble.qxztrw.model.NoteLabel

/** The labels a space shows until either partner renames, deletes or adds one. */
fun defaultNoteLabels(resources: Resources): List<NoteLabel> = listOf(
    NoteLabel(DefaultNoteLabelIds.SHOPPING, resources.getString(R.string.note_label_default_shopping), 1L),
    NoteLabel(DefaultNoteLabelIds.BUCKET_LIST, resources.getString(R.string.note_label_default_bucket_list), 2L),
    NoteLabel(DefaultNoteLabelIds.IDEAS, resources.getString(R.string.note_label_default_ideas), 3L)
)
