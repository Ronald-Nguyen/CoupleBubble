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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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
    authorName: String? = null,
    isSelected: Boolean,
    isSelecting: Boolean = isSelected,
    isUnread: Boolean = false,
    searchQuery: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val openItems = remember(note.items, searchQuery) {
        val items = NoteOrganizer.orderedItems(note.items).filterNot { it.checked }
        if (!searchQuery.isNullOrBlank()) {
            items.sortedByDescending { NoteOrganizer.highlightRanges(it.text, searchQuery).isNotEmpty() }
        } else {
            items
        }
    }
    val (checkedCount, totalCount) = remember(note.items) { NoteOrganizer.progress(note) }
    val notSelectedDesc = stringResource(R.string.note_not_selected)
    val selectedDesc = stringResource(R.string.note_selected)
    val startSelectionLabel = stringResource(R.string.note_start_selection)

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
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = startSelectionLabel
            )
            .semantics {
                if (isSelecting) {
                    this.selected = isSelected
                    this.stateDescription = if (isSelected) selectedDesc else notSelectedDesc
                }
            }
            .testTag("note_card_${note.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .size(18.dp)
                        .testTag("note_selected_${note.id}")
                )
            }
            if (labelName != null || note.pinned || isUnread) {
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
                    if (isUnread) {
                        val unreadDesc = stringResource(R.string.note_unread_changes)
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                                .semantics { contentDescription = unreadDesc }
                                .testTag("note_unread_dot_${note.id}")
                        )
                        if (note.pinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                        }
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
                    text = rememberHighlightedText(note.title, searchQuery),
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
                        text = rememberHighlightedText(note.body, searchQuery),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                NoteType.CHECKLIST -> ChecklistPreview(openItems = openItems, searchQuery = searchQuery)
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
                val authorDesc = authorName?.let { stringResource(R.string.note_created_by, it) }
                AuthorDot(
                    colorHex = authorColorHex,
                    contentDescription = authorDesc,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun rememberHighlightedText(
    text: String,
    query: String?
): AnnotatedString {
    val highlightStyle = SpanStyle(
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Bold,
        background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    )
    return remember(text, query, highlightStyle) {
        val ranges = NoteOrganizer.highlightRanges(text, query)
        if (ranges.isEmpty()) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                append(text)
                for (range in ranges) {
                    val start = range.first.coerceIn(0, text.length)
                    val end = (range.last + 1).coerceIn(0, text.length)
                    if (start < end) {
                        addStyle(highlightStyle, start, end)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistPreview(openItems: List<NoteItem>, searchQuery: String? = null) {
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
                text = rememberHighlightedText(item.text, searchQuery),
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
internal fun AuthorDot(
    colorHex: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val color = remember(colorHex) { parseColorHexToCompose(colorHex) }
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
            .testTag("note_author_dot")
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
