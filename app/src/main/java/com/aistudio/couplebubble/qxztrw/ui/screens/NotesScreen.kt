package com.aistudio.couplebubble.qxztrw.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.model.NoteCategory
import com.aistudio.couplebubble.qxztrw.model.NoteItem
import com.aistudio.couplebubble.qxztrw.model.NoteOrganizer
import com.aistudio.couplebubble.qxztrw.model.NoteType
import com.aistudio.couplebubble.qxztrw.model.SharedNote
import com.aistudio.couplebubble.qxztrw.ui.NotesEvent
import com.aistudio.couplebubble.qxztrw.ui.NotesUiState
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme

private const val MAX_PREVIEW_ITEMS = 5

@get:StringRes
internal val NoteCategory.labelRes: Int
    get() = when (this) {
        NoteCategory.SHOPPING -> R.string.note_category_shopping
        NoteCategory.BUCKET_LIST -> R.string.note_category_bucket_list
        NoteCategory.IDEAS -> R.string.note_category_ideas
    }

/** Notes tab: the overview grid, or the editor of the open note. */
@Composable
fun NotesScreen(
    state: NotesUiState,
    onEvent: (NotesEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Keeps the editor content while it animates out after the note was closed
    var lastOpenNote by remember { mutableStateOf(state.openNote) }
    state.openNote?.let { lastOpenNote = it }

    AnimatedContent(
        targetState = state.openNote?.id,
        transitionSpec = {
            (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                scaleIn(spring(stiffness = Spring.StiffnessLow), initialScale = 0.96f))
                .togetherWith(fadeOut(spring(stiffness = Spring.StiffnessMedium)))
        },
        label = "notes_editor_transition",
        modifier = modifier
    ) { openNoteId ->
        val note = lastOpenNote
        if (openNoteId != null && note != null && note.id == openNoteId) {
            NoteEditorScreen(
                note = note,
                authorColors = state.authorColors,
                showDeleteDialog = state.noteToDelete?.id == note.id,
                onEvent = onEvent,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            NotesOverview(state = state, onEvent = onEvent, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun NotesOverview(
    state: NotesUiState,
    onEvent: (NotesEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val gridState = rememberLazyStaggeredGridState()
    // Collapse the FAB to its icon once the user scrolls into the notes
    val isFabExpanded by remember {
        derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset < 120 }
    }
    val (pinnedNotes, otherNotes) = remember(state.notes) { state.notes.partition { it.pinned } }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(text = stringResource(R.string.add_note)) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (isFabExpanded) null else stringResource(R.string.add_note)
                    )
                },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEvent(NotesEvent.CreateNote)
                },
                expanded = isFabExpanded,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                modifier = Modifier.testTag("notes_fab")
            )
        }
    ) { contentPadding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            state = gridState,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .testTag("notes_grid")
        ) {
            item(key = "header", span = StaggeredGridItemSpan.FullLine) {
                NotesHeader(
                    selectedFilter = state.categoryFilter,
                    onFilterSelected = { onEvent(NotesEvent.FilterSelected(it)) }
                )
            }

            when {
                !state.hasAnyNotes -> item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
                    EmptyNotesCard(onCreateNote = { onEvent(NotesEvent.CreateNote) })
                }
                state.notes.isEmpty() -> item(key = "filter_empty", span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = stringResource(R.string.notes_filter_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .testTag("notes_filter_empty")
                    )
                }
                else -> {
                    if (pinnedNotes.isNotEmpty()) {
                        item(key = "label_pinned", span = StaggeredGridItemSpan.FullLine) {
                            NotesSectionLabel(R.string.notes_section_pinned)
                        }
                        items(pinnedNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                authorColorHex = note.createdBy?.let { state.authorColors[it] },
                                onClick = { onEvent(NotesEvent.OpenNote(note.id)) }
                            )
                        }
                        if (otherNotes.isNotEmpty()) {
                            item(key = "label_others", span = StaggeredGridItemSpan.FullLine) {
                                NotesSectionLabel(R.string.notes_section_others)
                            }
                        }
                    }
                    items(otherNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            authorColorHex = note.createdBy?.let { state.authorColors[it] },
                            onClick = { onEvent(NotesEvent.OpenNote(note.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesHeader(
    selectedFilter: NoteCategory?,
    onFilterSelected: (NoteCategory?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val filters = remember { listOf<NoteCategory?>(null) + NoteCategory.entries }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.notes_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.notes_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 14.dp, bottom = 4.dp)
        ) {
            filters.forEach { category ->
                val isSelected = category == selectedFilter
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onFilterSelected(category)
                        }
                    },
                    label = { Text(stringResource(category?.labelRes ?: R.string.notes_filter_all)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.dp
                    ),
                    modifier = Modifier.testTag("notes_filter_${category?.name ?: "ALL"}")
                )
            }
        }
    }
}

@Composable
private fun NotesSectionLabel(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes).uppercase(),
        fontSize = 12.sp,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun EmptyNotesCard(onCreateNote: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .testTag("empty_notes_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCreateNote()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier.testTag("add_first_note_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.add_first_note), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: SharedNote,
    authorColorHex: String?,
    onClick: () -> Unit
) {
    val openItems = remember(note.items) { NoteOrganizer.orderedItems(note.items).filterNot { it.checked } }
    val (checkedCount, totalCount) = remember(note.items) { NoteOrganizer.progress(note) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("note_card_${note.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (note.category != null || note.pinned) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val category = note.category
                    if (category != null) {
                        Text(
                            text = stringResource(category.labelRes).uppercase(),
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

internal val previewAuthorColors = mapOf("uid_alex" to "#E65D2E", "uid_sam" to "#4ECDC4")

internal val previewNotes = listOf(
    SharedNote(
        id = "n1",
        title = "Wocheneinkauf",
        type = NoteType.CHECKLIST,
        category = NoteCategory.SHOPPING,
        pinned = true,
        createdBy = "uid_alex",
        items = listOf(
            NoteItem("i1", "Hafermilch", checked = true, createdBy = "uid_alex", position = 1),
            NoteItem("i2", "Feta & Oliven", createdBy = "uid_sam", position = 2),
            NoteItem("i3", "Basilikum", createdBy = "uid_alex", position = 3),
            NoteItem("i4", "Blumen für Oma", createdBy = "uid_sam", position = 4)
        ),
        updatedAt = 3
    ),
    SharedNote(
        id = "n2",
        title = "Irgendwann zusammen",
        type = NoteType.CHECKLIST,
        category = NoteCategory.BUCKET_LIST,
        createdBy = "uid_sam",
        items = listOf(
            NoteItem("b1", "Polarlichter in Norwegen", createdBy = "uid_sam", position = 1),
            NoteItem("b2", "Tanzkurs (Salsa?)", createdBy = "uid_alex", position = 2),
            NoteItem("b3", "Eine Nacht im Baumhaus", checked = true, createdBy = "uid_sam", position = 3)
        ),
        updatedAt = 2
    ),
    SharedNote(
        id = "n3",
        title = "Geschenkideen Jahrestag",
        body = "Fotobuch von Lissabon\nKochkurs für zwei\nDie Platte aus dem kleinen Laden am Hafen",
        category = NoteCategory.IDEAS,
        createdBy = "uid_alex",
        updatedAt = 1
    ),
    SharedNote(
        id = "n4",
        title = "WLAN bei deinen Eltern",
        body = "Netz: Gartenhaus\nPasswort steht auf dem Router im Flur",
        createdBy = "uid_sam",
        updatedAt = 0
    )
)

@Preview(name = "Notes Light", showBackground = true)
@Composable
private fun NotesScreenPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        NotesScreen(
            state = NotesUiState(notes = previewNotes, hasAnyNotes = true, authorColors = previewAuthorColors),
            onEvent = {}
        )
    }
}

@Preview(name = "Notes Dark", showBackground = true)
@Composable
private fun NotesScreenDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        NotesScreen(
            state = NotesUiState(notes = previewNotes, hasAnyNotes = true, authorColors = previewAuthorColors),
            onEvent = {}
        )
    }
}

@Preview(name = "Notes Empty Light", showBackground = true)
@Composable
private fun NotesScreenEmptyPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        NotesScreen(state = NotesUiState(), onEvent = {})
    }
}

@Preview(name = "Notes Empty Dark", showBackground = true)
@Composable
private fun NotesScreenEmptyDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        NotesScreen(state = NotesUiState(), onEvent = {})
    }
}
