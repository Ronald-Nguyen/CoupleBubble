package com.aistudio.couplebubble.qxztrw.ui.screens.notes

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.model.DefaultNoteLabelIds
import com.aistudio.couplebubble.qxztrw.domain.model.NoteItem
import com.aistudio.couplebubble.qxztrw.domain.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.domain.model.NoteType
import com.aistudio.couplebubble.qxztrw.domain.model.SharedNote
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme

/** Notes tab: the overview grid, or the editor of the open note. */
@Composable
fun NotesScreen(
    state: NotesUiState,
    onEvent: (NotesEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState()
) {
    // Keeps the editor content while it animates out after the note was closed
    var lastOpenNote by remember { mutableStateOf(state.openNote) }
    state.openNote?.let { lastOpenNote = it }
    val saveableStateHolder = rememberSaveableStateHolder()

    Box(modifier = modifier) {
        AnimatedContent(
            targetState = state.openNote?.id,
            transitionSpec = {
                (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                    scaleIn(spring(stiffness = Spring.StiffnessLow), initialScale = 0.96f))
                    .togetherWith(fadeOut(spring(stiffness = Spring.StiffnessMedium)))
            },
            label = "notes_editor_transition",
            modifier = Modifier.fillMaxSize()
        ) { openNoteId ->
            val note = lastOpenNote
            if (openNoteId != null && note != null && note.id == openNoteId) {
                saveableStateHolder.SaveableStateProvider("editor_$openNoteId") {
                    NoteEditorScreen(
                        note = note,
                        labels = state.labels,
                        authorColors = state.authorColors,
                        onEvent = onEvent,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                saveableStateHolder.SaveableStateProvider("overview") {
                    NotesOverview(
                        state = state,
                        onEvent = onEvent,
                        snackbarHostState = snackbarHostState,
                        gridState = gridState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // The editor has no Scaffold; its snackbars sit above the keyboard
        if (state.openNote != null) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
            )
        }
    }

    state.labelEditor?.let { editor ->
        ManageLabelsDialog(editor = editor, labelUsage = state.labelUsage, onEvent = onEvent)
    }
}

@Composable
private fun NotesOverview(
    state: NotesUiState,
    onEvent: (NotesEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState()
) {
    val haptic = LocalHapticFeedback.current
    // Collapse the FAB to its icon once the user scrolls into the notes
    val isFabExpanded by remember {
        derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset < 120 }
    }
    val (pinnedNotes, otherNotes) = remember(state.notes) { state.notes.partition { it.pinned } }

    BackHandler(enabled = state.isSelecting) { onEvent(NotesEvent.ClearSelection) }

    // While selecting, a tap toggles the note instead of opening it
    val onNoteClick: (SharedNote) -> Unit = { note ->
        if (state.isSelecting) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onEvent(NotesEvent.ToggleNoteSelection(note.id))
        } else {
            onEvent(NotesEvent.OpenNote(note.id))
        }
    }
    val onNoteLongClick: (SharedNote) -> Unit = { note ->
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onEvent(NotesEvent.ToggleNoteSelection(note.id))
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (state.isSelecting) {
                NotesSelectionBar(
                    selectedCount = state.selectedNoteIds.size,
                    onClear = { onEvent(NotesEvent.ClearSelection) },
                    onDelete = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEvent(NotesEvent.DeleteSelected)
                    }
                )
            }
        },
        floatingActionButton = {
            if (!state.isSelecting) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (isFabExpanded) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.add_checklist),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEvent(NotesEvent.CreateChecklist)
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.secondary,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                            modifier = Modifier.testTag("notes_checklist_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Checklist,
                                contentDescription = stringResource(R.string.add_checklist),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
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
            }
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
                    labels = state.labels,
                    selectedFilter = state.labelFilter,
                    onFilterSelected = { onEvent(NotesEvent.FilterSelected(it)) },
                    onManageLabels = { onEvent(NotesEvent.OpenLabelManager) }
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
                                labelName = note.labelId?.let { state.labelNames[it] },
                                authorColorHex = note.createdBy?.let { state.authorColors[it] },
                                isSelected = note.id in state.selectedNoteIds,
                                onClick = { onNoteClick(note) },
                                onLongClick = { onNoteLongClick(note) }
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
                            labelName = note.labelId?.let { state.labelNames[it] },
                            authorColorHex = note.createdBy?.let { state.authorColors[it] },
                            isSelected = note.id in state.selectedNoteIds,
                            onClick = { onNoteClick(note) },
                            onLongClick = { onNoteLongClick(note) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesHeader(
    labels: List<NoteLabel>,
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit,
    onManageLabels: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val filters = remember(labels) { listOf<NoteLabel?>(null) + labels }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.notes_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 14.dp, bottom = 4.dp)
        ) {
            filters.forEach { label ->
                val isSelected = label?.id == selectedFilter
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onFilterSelected(label?.id)
                        }
                    },
                    label = { Text(label?.name ?: stringResource(R.string.notes_filter_all)) },
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
                    modifier = Modifier.testTag("notes_filter_${label?.id ?: "ALL"}")
                )
            }
            AssistChip(
                onClick = onManageLabels,
                label = { Text(stringResource(R.string.manage_labels_chip)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                },
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier.testTag("manage_labels_chip")
            )
        }
    }
}

@Composable
private fun NotesSelectionBar(
    selectedCount: Int,
    onClear: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notes_selection_bar")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            IconButton(onClick = onClear, modifier = Modifier.testTag("notes_selection_clear")) {
                Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.notes_selection_clear))
            }
            Text(
                text = pluralStringResource(R.plurals.notes_selected_count, selectedCount, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
            IconButton(onClick = onDelete, modifier = Modifier.testTag("notes_selection_delete")) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.notes_selection_delete),
                    tint = MaterialTheme.colorScheme.error
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

internal val previewLabels = listOf(
    NoteLabel(DefaultNoteLabelIds.SHOPPING, "Einkauf", 1),
    NoteLabel(DefaultNoteLabelIds.BUCKET_LIST, "Bucket List", 2),
    NoteLabel("label_travel", "Lissabon 2027", 3)
)

internal val previewAuthorColors = mapOf("uid_alex" to "#E65D2E", "uid_sam" to "#4ECDC4")

internal val previewNotes = listOf(
    SharedNote(
        id = "n1",
        title = "Wocheneinkauf",
        type = NoteType.CHECKLIST,
        labelId = DefaultNoteLabelIds.SHOPPING,
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
        labelId = DefaultNoteLabelIds.BUCKET_LIST,
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
        labelId = "label_travel",
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
            state = NotesUiState(
                notes = previewNotes,
                hasAnyNotes = true,
                labels = previewLabels,
                authorColors = previewAuthorColors
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "Notes Dark", showBackground = true)
@Composable
private fun NotesScreenDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        NotesScreen(
            state = NotesUiState(
                notes = previewNotes,
                hasAnyNotes = true,
                labels = previewLabels,
                authorColors = previewAuthorColors
            ),
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
