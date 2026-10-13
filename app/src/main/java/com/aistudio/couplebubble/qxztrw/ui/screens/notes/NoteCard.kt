package com.aistudio.couplebubble.qxztrw.ui.screens.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.model.NoteItem
import com.aistudio.couplebubble.qxztrw.domain.model.NoteOrganizer
import com.aistudio.couplebubble.qxztrw.domain.model.NoteType
import com.aistudio.couplebubble.qxztrw.domain.model.SharedNote
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import com.aistudio.couplebubble.qxztrw.ui.theme.parseColorHexToCompose

private const val MAX_PREVIEW_ITEMS = 5

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NoteCard(
    note: SharedNote,
    labelName: String?,
    authorColorHex: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val openItems = remember(note.items) { NoteOrganizer.orderedItems(note.items).filterNot { it.checked } }
    val (checkedCount, totalCount) = remember(note.items) { NoteOrganizer.progress(note) }

    Card(
        shape = RoundedCornerShape(18.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("note_card_${note.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.note_selected),
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .size(18.dp)
                        .testTag("note_selected_${note.id}")
                )
            }
            if (labelName != null || note.pinned) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (labelName != null) {
                        Text(
                            text = labelName.uppercase(),
                            fontSize = 11.sp,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (note.pinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = stringResource(R.string.note_pinned),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            when {
                note.title.isNotBlank() -> Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                note.isEmpty -> Text(
                    text = stringResource(R.string.note_untitled),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (note.type) {
                NoteType.TEXT -> if (note.body.isNotBlank()) {
                    Text(
                        text = note.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                NoteType.CHECKLIST -> ChecklistPreview(openItems = openItems)
            }

            if (note.type == NoteType.CHECKLIST && totalCount > 0) {
                Text(
                    text = stringResource(R.string.note_progress, checkedCount, totalCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                LinearProgressIndicator(
                    progress = { checkedCount.toFloat() / totalCount },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    strokeCap = StrokeCap.Round
                )
            }

            if (authorColorHex != null) {
                AuthorDot(colorHex = authorColorHex, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun ChecklistPreview(openItems: List<NoteItem>) {
    openItems.take(MAX_PREVIEW_ITEMS).forEach { item ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    val hiddenCount = openItems.size - MAX_PREVIEW_ITEMS
    if (hiddenCount > 0) {
        Text(
            text = pluralStringResource(R.plurals.note_more_items, hiddenCount, hiddenCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Small dot in the accent color of the partner who wrote a note or item. */
@Composable
internal fun AuthorDot(colorHex: String, modifier: Modifier = Modifier) {
    val color = remember(colorHex) { parseColorHexToCompose(colorHex) }
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Preview(name = "Note Card Light", showBackground = true)
@Composable
private fun NoteCardPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        NoteCard(
            note = previewNotes.first(),
            labelName = previewLabels.first().name,
            authorColorHex = previewAuthorColors["uid_alex"],
            isSelected = false,
            onClick = {},
            onLongClick = {}
        )
    }
}

@Preview(name = "Note Card Dark (selected)", showBackground = true)
@Composable
private fun NoteCardDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        NoteCard(
            note = previewNotes[2],
            labelName = previewLabels.last().name,
            authorColorHex = previewAuthorColors["uid_alex"],
            isSelected = true,
            onClick = {},
            onLongClick = {}
        )
    }
}
