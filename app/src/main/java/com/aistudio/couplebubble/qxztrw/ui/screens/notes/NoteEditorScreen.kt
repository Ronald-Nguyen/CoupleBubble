package com.aistudio.couplebubble.qxztrw.ui.screens.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import java.util.UUID
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.model.InputField
import com.aistudio.couplebubble.qxztrw.domain.model.InputRules
import com.aistudio.couplebubble.qxztrw.domain.model.NoteItem
import com.aistudio.couplebubble.qxztrw.domain.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.domain.model.NoteOrganizer
import com.aistudio.couplebubble.qxztrw.domain.model.NoteType
import com.aistudio.couplebubble.qxztrw.domain.model.SharedNote
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme

/** Full-screen editor of one shared note; every change goes straight to the ViewModel. */
@Composable
fun NoteEditorScreen(
    note: SharedNote,
    labels: List<NoteLabel>,
    authorColors: Map<String, String>,
    onEvent: (NotesEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val orderedItems = remember(note.items) { NoteOrganizer.orderedItems(note.items) }
    val (checkedItems, openItems) = remember(orderedItems) { orderedItems.partition { it.checked } }
    // Focus the title right away for a fresh note, but not when opening an existing one
    val focusTitleOnOpen = remember(note.id) { note.isEmpty }
    val labelName = remember(note.labelId, labels) { labels.firstOrNull { it.id == note.labelId }?.name }
    val listState = rememberLazyListState()
    val reorderState = remember(note.id) { ChecklistReorderState(listState, openItems) }
    LaunchedEffect(reorderState, openItems) { reorderState.sync(openItems) }
    var isCheckedExpanded by rememberSaveable { mutableStateOf(true) }
    var pendingFocusItemId by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    BackHandler { onEvent(NotesEvent.CloseNote) }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .testTag("note_editor")
    ) {
        NoteEditorTopBar(note = note, labels = labels, onEvent = onEvent)

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 40.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            labelName?.let { name ->
                item(key = "label") {
                    Text(
                        text = name.uppercase(),
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            item(key = "title") {
                val focusRequester = remember { FocusRequester() }
                SyncedTextField(
                    remoteText = note.title,
                    resetKey = note.id,
                    onTextChange = { onEvent(NotesEvent.TitleChanged(it)) },
                    placeholder = stringResource(R.string.note_title_placeholder),
                    limit = InputField.NOTE_TITLE,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp, bottom = 12.dp)
                        .focusRequester(focusRequester)
                        .testTag("note_title_field")
                )
                LaunchedEffect(focusTitleOnOpen) {
                    if (focusTitleOnOpen) focusRequester.requestFocus()
                }
            }

            when (note.type) {
                NoteType.TEXT -> item(key = "body") {
                    SyncedTextField(
                        remoteText = note.body,
                        resetKey = note.id to note.type,
                        onTextChange = { onEvent(NotesEvent.BodyChanged(it)) },
                        placeholder = stringResource(R.string.note_body_placeholder),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp)
                            .padding(end = 8.dp)
                            .testTag("note_body_field")
                    )
                }
                NoteType.CHECKLIST -> checklistItems(
                    reorderState = reorderState,
                    checkedItems = checkedItems,
                    authorColors = authorColors,
                    isCheckedExpanded = isCheckedExpanded,
                    onToggleCheckedExpanded = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isCheckedExpanded = !isCheckedExpanded
                    },
                    pendingFocusItemId = pendingFocusItemId,
                    onFocusItem = { pendingFocusItemId = it },
                    onFocusConsumed = { pendingFocusItemId = null },
                    onEvent = onEvent,
                    onSwap = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                )
            }
        }
    }

}

@Composable
private fun NoteEditorTopBar(
    note: SharedNote,
    labels: List<NoteLabel>,
    onEvent: (NotesEvent) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        IconButton(
            onClick = { onEvent(NotesEvent.CloseNote) },
            modifier = Modifier.testTag("note_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.note_back)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onEvent(NotesEvent.TogglePinned)
            },
            modifier = Modifier.testTag("note_pin_button")
        ) {
            Icon(
                imageVector = if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = stringResource(if (note.pinned) R.string.note_unpin else R.string.note_pin),
                tint = if (note.pinned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        NoteLabelMenu(note = note, labels = labels, onEvent = onEvent)

        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onEvent(NotesEvent.ConvertNoteType)
            },
            modifier = Modifier.testTag("note_convert_button")
        ) {
            Icon(
                imageVector = if (note.type == NoteType.TEXT) Icons.Filled.Checklist else Icons.AutoMirrored.Outlined.Notes,
                contentDescription = stringResource(
                    if (note.type == NoteType.TEXT) R.string.note_convert_to_checklist else R.string.note_convert_to_text
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        NoteMoreMenu(onEvent = onEvent)
    }
}

/** Picks the note's label, or opens the label editor. */
@Composable
private fun NoteLabelMenu(note: SharedNote, labels: List<NoteLabel>, onEvent: (NotesEvent) -> Unit) {
    val haptic = LocalHapticFeedback.current
    var isLabelMenuOpen by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { isLabelMenuOpen = true },
            modifier = Modifier.testTag("note_label_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Label,
                contentDescription = stringResource(R.string.note_choose_label),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = isLabelMenuOpen,
            onDismissRequest = { isLabelMenuOpen = false }
        ) {
            (listOf<NoteLabel?>(null) + labels).forEach { label ->
                // A note whose label was deleted counts as unlabeled
                val isSelected = label?.id == note.labelId || (label == null && labels.none { it.id == note.labelId })
                DropdownMenuItem(
                    text = { Text(label?.name ?: stringResource(R.string.note_label_none)) },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        isLabelMenuOpen = false
                        if (!isSelected) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEvent(NotesEvent.LabelChanged(label?.id))
                        }
                    },
                    modifier = Modifier.testTag("note_label_${label?.id ?: "NONE"}")
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            DropdownMenuItem(
                text = { Text(stringResource(R.string.manage_labels_menu)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = {
                    isLabelMenuOpen = false
                    onEvent(NotesEvent.OpenLabelManager)
                },
                modifier = Modifier.testTag("note_manage_labels")
            )
        }
    }
}

@Composable
private fun NoteMoreMenu(onEvent: (NotesEvent) -> Unit) {
    val haptic = LocalHapticFeedback.current
    var isMoreMenuOpen by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { isMoreMenuOpen = true },
            modifier = Modifier.testTag("note_more_button")
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.note_more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = isMoreMenuOpen,
            onDismissRequest = { isMoreMenuOpen = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.note_delete)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    isMoreMenuOpen = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEvent(NotesEvent.DeleteNote)
                },
                modifier = Modifier.testTag("note_delete_menu_item")
            )
        }
    }
}

/** Open items (draggable by their handle), the "add item" row, then the checked items under their header. */
private fun LazyListScope.checklistItems(
    reorderState: ChecklistReorderState,
    checkedItems: List<NoteItem>,
    authorColors: Map<String, String>,
    isCheckedExpanded: Boolean,
    onToggleCheckedExpanded: () -> Unit,
    pendingFocusItemId: String?,
    onFocusItem: (String) -> Unit,
    onFocusConsumed: () -> Unit,
    onEvent: (NotesEvent) -> Unit,
    onSwap: () -> Unit
) {
    items(reorderState.order, key = { it.id }) { item ->
        val isDragged = reorderState.draggedId == item.id
        val itemFocusRequester = remember(item.id) { FocusRequester() }

        LaunchedEffect(pendingFocusItemId) {
            if (pendingFocusItemId == item.id) {
                itemFocusRequester.requestFocus()
                onFocusConsumed()
            }
        }

        ChecklistItemRow(
            item = item,
            authorColorHex = item.createdBy?.let { authorColors[it] },
            onEvent = onEvent,
            dragHandle = DragHandleCallbacks(
                onDragStart = { reorderState.start(item.id) },
                onDrag = { delta ->
                    if (reorderState.drag(delta)) {
                        onSwap()
                    }
                },
                onDragEnd = { reorderState.end()?.let { onEvent(NotesEvent.ReorderItems(it)) } }
            ),
            focusRequester = itemFocusRequester,
            onEnter = {
                val newId = UUID.randomUUID().toString()
                onFocusItem(newId)
                reorderState.insertAfter(item.id, NoteItem(id = newId, position = item.position + 1L))
                onEvent(NotesEvent.InsertItemAfter(item.id, newId))
            },
            onBackspaceOnEmpty = {
                val currentOrder = reorderState.order
                val currentIndex = currentOrder.indexOfFirst { it.id == item.id }
                if (currentIndex > 0) {
                    val prevItem = currentOrder[currentIndex - 1]
                    onFocusItem(prevItem.id)
                }
                reorderState.remove(item.id)
                onEvent(NotesEvent.DeleteItem(item.id))
            },
            modifier = if (isDragged) {
                Modifier
                    .zIndex(1f)
                    .graphicsLayer { translationY = reorderState.dragOffset }
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            } else {
                Modifier.animateItem()
            }
        )
    }
    item(key = "add_item") {
        AddItemRow(
            onAddItem = { onEvent(NotesEvent.AddItem(it)) },
            onDraftChanged = { onEvent(NotesEvent.NewItemDraftChanged(it)) },
            modifier = Modifier.animateItem()
        )
    }
    if (checkedItems.isNotEmpty()) {
        item(key = "checked_header") {
            CheckedItemsHeader(
                count = checkedItems.size,
                expanded = isCheckedExpanded,
                onToggleExpand = onToggleCheckedExpanded,
                onClearChecked = { onEvent(NotesEvent.ClearCheckedItems) },
                modifier = Modifier.animateItem(
                    fadeInSpec = spring(stiffness = Spring.StiffnessLow),
                    placementSpec = spring(stiffness = Spring.StiffnessLow),
                    fadeOutSpec = spring(stiffness = Spring.StiffnessLow)
                )
            )
        }
        if (isCheckedExpanded) {
            items(checkedItems, key = { it.id }) { item ->
                ChecklistItemRow(
                    item = item,
                    authorColorHex = item.createdBy?.let { authorColors[it] },
                    onEvent = onEvent,
                    modifier = Modifier.animateItem(
                        fadeInSpec = spring(stiffness = Spring.StiffnessLow),
                        placementSpec = spring(stiffness = Spring.StiffnessLow),
                        fadeOutSpec = spring(stiffness = Spring.StiffnessLow)
                    )
                )
            }
        }
    }
}

/** Local order of the open checklist items while one of them is dragged by its handle. */
private class ChecklistReorderState(private val listState: LazyListState, initial: List<NoteItem>) {
    var order by mutableStateOf(initial)
        private set
    var draggedId by mutableStateOf<String?>(null)
        private set
    var dragOffset by mutableFloatStateOf(0f)
        private set
    private var latest = initial
    private var orderAtStart = initial

    fun sync(items: List<NoteItem>) {
        latest = items
        if (draggedId == null) order = items
    }

    fun insertAfter(afterId: String, newItem: NoteItem) {
        val idx = order.indexOfFirst { it.id == afterId }
        val newOrder = if (idx != -1) {
            order.toMutableList().apply { add(idx + 1, newItem) }
        } else {
            order + newItem
        }
        order = newOrder
        latest = newOrder
    }

    fun remove(itemId: String) {
        val newOrder = order.filterNot { it.id == itemId }
        order = newOrder
        latest = newOrder
    }

    fun start(itemId: String) {
        orderAtStart = order
        draggedId = itemId
        dragOffset = 0f
    }

    /** Moves the dragged item; returns true when it swapped places with a neighbor. */
    fun drag(delta: Float): Boolean {
        val id = draggedId ?: return false
        dragOffset += delta
        val visible = listState.layoutInfo.visibleItemsInfo
        val current = visible.firstOrNull { it.key == id } ?: return false
        val center = current.offset + dragOffset + current.size / 2f
        val target = visible.firstOrNull { info ->
            info.key != id && order.any { it.id == info.key } &&
                center >= info.offset && center <= info.offset + info.size
        } ?: return false
        val from = order.indexOfFirst { it.id == id }
        val to = order.indexOfFirst { it.id == target.key }
        order = order.toMutableList().apply { add(to, removeAt(from)) }
        dragOffset += current.offset - target.offset
        return true
    }

    /** Ends the drag; returns the new id order if it changed. */
    fun end(): List<String>? {
        draggedId = null
        dragOffset = 0f
        val changed = order.map { it.id } != orderAtStart.map { it.id }
        if (!changed) order = latest
        return if (changed) order.map { it.id } else null
    }
}

private class DragHandleCallbacks(
    val onDragStart: () -> Unit,
    val onDrag: (Float) -> Unit,
    val onDragEnd: () -> Unit
)

@Composable
private fun ChecklistItemRow(
    item: NoteItem,
    authorColorHex: String?,
    onEvent: (NotesEvent) -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: DragHandleCallbacks? = null,
    focusRequester: FocusRequester? = null,
    onEnter: (() -> Unit)? = null,
    onBackspaceOnEmpty: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val baseStyle = MaterialTheme.typography.bodyLarge
    val textStyle = if (item.checked) {
        baseStyle.copy(
            textDecoration = TextDecoration.LineThrough,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    } else {
        baseStyle
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .testTag("note_item_${item.id}")
    ) {
        if (dragHandle != null) {
            val callbacks by rememberUpdatedState(dragHandle)
            Icon(
                imageVector = Icons.Filled.DragIndicator,
                contentDescription = stringResource(R.string.note_reorder_item),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(24.dp)
                    .pointerInput(item.id) {
                        detectDragGestures(
                            onDragStart = { callbacks.onDragStart() },
                            onDragEnd = { callbacks.onDragEnd() },
                            onDragCancel = { callbacks.onDragEnd() },
                            onDrag = { change, amount ->
                                change.consume()
                                callbacks.onDrag(amount.y)
                            }
                        )
                    }
                    .testTag("note_item_drag_${item.id}")
            )
        }
        Checkbox(
            checked = item.checked,
            onCheckedChange = { checked ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onEvent(NotesEvent.ItemCheckedChanged(item.id, checked))
            },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.secondary,
                checkmarkColor = MaterialTheme.colorScheme.onSecondary
            ),
            modifier = Modifier
                .semantics { contentDescription = item.text }
                .testTag("note_item_checkbox_${item.id}")
        )
        if (authorColorHex != null) {
            AuthorDot(colorHex = authorColorHex)
            Spacer(modifier = Modifier.width(10.dp))
        }
        SyncedTextField(
            remoteText = item.text,
            resetKey = item.id,
            onTextChange = { onEvent(NotesEvent.ItemTextChanged(item.id, it)) },
            placeholder = stringResource(R.string.note_item_placeholder),
            textStyle = textStyle,
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            focusRequester = focusRequester,
            onEnter = onEnter,
            onBackspaceOnEmpty = onBackspaceOnEmpty,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { onEvent(NotesEvent.DeleteItem(item.id)) },
            modifier = Modifier
                .size(40.dp)
                .testTag("note_item_delete_${item.id}")
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.note_delete_item),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AddItemRow(
    onAddItem: (String) -> Unit,
    onDraftChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by rememberSaveable { mutableStateOf("") }
    // Enter adds the item and keeps the keyboard open for the next one; leaving the field adds it too
    val submit = {
        if (text.isNotBlank()) {
            onAddItem(text)
            text = ""
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        IconButton(
            onClick = submit,
            enabled = text.isNotBlank(),
            modifier = Modifier.testTag("add_item_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.note_add_item_placeholder),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                onDraftChanged(it)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.note_add_item_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .onFocusChanged { if (!it.isFocused) submit() }
                .testTag("add_item_field")
        )
    }
}

@Composable
private fun CheckedItemsHeader(
    count: Int,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onClearChecked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "checkedItemsArrow"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, end = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = onToggleExpand)
                    .padding(vertical = 4.dp)
                    .testTag("toggle_checked_section")
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(arrowRotation)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.note_checked_section, count).uppercase(),
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClearChecked()
                },
                modifier = Modifier.testTag("clear_checked_button")
            ) {
                Text(
                    text = stringResource(R.string.note_clear_checked),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun SyncedTextField(
    remoteText: String,
    resetKey: Any,
    onTextChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    limit: InputField? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester? = null,
    onEnter: (() -> Unit)? = null,
    onBackspaceOnEmpty: (() -> Unit)? = null
) {
    var value by remember(resetKey) { mutableStateOf(TextFieldValue(remoteText, TextRange(remoteText.length))) }
    var isFocused by remember { mutableStateOf(false) }
    val resolvedStyle = if (textStyle.color == Color.Unspecified) {
        textStyle.copy(color = MaterialTheme.colorScheme.onSurface)
    } else {
        textStyle
    }

    LaunchedEffect(remoteText) {
        if (!isFocused && remoteText != value.text) {
            value = TextFieldValue(remoteText, TextRange(remoteText.length))
        }
    }

    val keyModifier = if (onEnter != null || onBackspaceOnEmpty != null) {
        Modifier.onPreviewKeyEvent { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                if (keyEvent.key == Key.Backspace || keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DEL) {
                    if (value.text.isEmpty() && onBackspaceOnEmpty != null) {
                        onBackspaceOnEmpty()
                        return@onPreviewKeyEvent true
                    }
                } else if (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter || keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                    if (onEnter != null) {
                        onEnter()
                        return@onPreviewKeyEvent true
                    }
                }
            }
            false
        }
    } else {
        Modifier
    }

    val effectiveActions = if (onEnter != null) {
        KeyboardActions(
            onDone = { onEnter() },
            onNext = { onEnter() }
        )
    } else {
        keyboardActions
    }

    val effectiveOptions = if (onEnter != null) {
        keyboardOptions.copy(imeAction = ImeAction.Next)
    } else {
        keyboardOptions
    }

    BasicTextField(
        value = value,
        onValueChange = { input ->
            // Text beyond the limit is cut while typing; there is no save button to disable here
            val newValue = if (limit != null && input.text.length > limit.maxLength) {
                input.copy(text = input.text.take(limit.maxLength), selection = TextRange(limit.maxLength))
            } else {
                input
            }
            val textChanged = newValue.text != value.text
            value = newValue
            if (textChanged) onTextChange(newValue.text)
        },
        textStyle = resolvedStyle,
        singleLine = singleLine,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
        keyboardOptions = effectiveOptions,
        keyboardActions = effectiveActions,
        decorationBox = { innerTextField ->
            Column {
                Box {
                    if (value.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = resolvedStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
                if (limit != null && isFocused && InputRules.showsCounter(limit, value.text)) {
                    Text(
                        text = stringResource(R.string.input_counter, value.text.length, limit.maxLength),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .then(keyModifier)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    value = value.copy(selection = TextRange(value.text.length))
                }
            }
    )
}

@Preview(name = "Note Editor Checklist Light", showBackground = true)
@Composable
private fun NoteEditorChecklistPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        NoteEditorScreen(
            note = previewNotes.first(),
            labels = previewLabels,
            authorColors = previewAuthorColors,
            onEvent = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(name = "Note Editor Checklist Dark", showBackground = true)
@Composable
private fun NoteEditorChecklistDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        NoteEditorScreen(
            note = previewNotes[1],
            labels = previewLabels,
            authorColors = previewAuthorColors,
            onEvent = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(name = "Note Editor Text Light", showBackground = true)
@Composable
private fun NoteEditorTextPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        NoteEditorScreen(
            note = previewNotes[2],
            labels = previewLabels,
            authorColors = previewAuthorColors,
            onEvent = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(name = "Note Editor Text Dark", showBackground = true)
@Composable
private fun NoteEditorTextDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        NoteEditorScreen(
            note = previewNotes[3],
            labels = previewLabels,
            authorColors = previewAuthorColors,
            onEvent = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
