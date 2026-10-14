package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import com.aistudio.couplebubble.qxztrw.ui.components.rememberInitialFocus
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.imePadding
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.GermanLongDate
import com.aistudio.couplebubble.qxztrw.ui.components.DateFieldButton
import com.aistudio.couplebubble.qxztrw.ui.components.selectableDatesBetween
import com.aistudio.couplebubble.qxztrw.domain.model.InputField
import com.aistudio.couplebubble.qxztrw.domain.model.InputRules
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.ui.components.DiscardChangesDialog
import com.aistudio.couplebubble.qxztrw.ui.components.MemoryDatePickerDialog
import com.aistudio.couplebubble.qxztrw.ui.components.ValidatedTextField
import com.aistudio.couplebubble.qxztrw.ui.preview.PreviewData
import com.aistudio.couplebubble.qxztrw.ui.screens.home.OwnPhotoEdit
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * Adds a new moment ([memory] = `null`) or edits [memory]. Each partner can only change their own photo slot;
 * the partner's slot is shown read-only and kept as it is on save.
 * [onSave] receives title, date and note plus what happened to the current user's own photo. Without a known
 * slot ([editableRole] `null`) both photos stay read-only.
 * The checkbox sets [Memory.countsAsMilestone], the same switch as the „Momente“ section of the milestone settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemoryFormDialog(
    memory: Memory?,
    partner1Name: String,
    partner2Name: String,
    partner1Color: Long,
    partner2Color: Long,
    editableRole: PartnerRole?,
    initialPhotoUri: Uri? = null,
    onDismiss: () -> Unit,
    onSave: (memory: Memory, ownPhoto: OwnPhotoEdit) -> Unit
) {
    val isNew = memory == null
    var title by remember { mutableStateOf(memory?.title.orEmpty()) }
    var selectedDate by remember { mutableStateOf(memory?.date ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf(memory?.note.orEmpty()) }
    var countsAsMilestone by remember { mutableStateOf(memory?.countsAsMilestone ?: false) }
    val haptic = LocalHapticFeedback.current

    var selectedImage1Uri by remember {
        mutableStateOf<Uri?>(if (editableRole == PartnerRole.PARTNER_1) initialPhotoUri else null)
    }
    var selectedImage2Uri by remember {
        mutableStateOf<Uri?>(if (editableRole == PartnerRole.PARTNER_2) initialPhotoUri else null)
    }
    var currentUrl1 by remember { mutableStateOf(memory?.effectivePartner1Image) }
    var currentUrl2 by remember { mutableStateOf(memory?.effectivePartner2Image) }

    val initialDraft = remember {
        MemoryFormDraft(
            title, selectedDate, note, countsAsMilestone, currentUrl1, currentUrl2,
            pickedPhoto1 = selectedImage1Uri != null,
            pickedPhoto2 = selectedImage2Uri != null
        )
    }
    var showDiscardPrompt by remember { mutableStateOf(false) }
    val isValid = InputRules.isValid(InputField.MEMORY_TITLE, title) && InputRules.isValid(InputField.MEMORY_NOTE, note)

    val photoPicker1 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImage1Uri = uri
    }

    val photoPicker2 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImage2Uri = uri
    }

    AlertDialog(
        onDismissRequest = {
            val hasChanges = MemoryFormDraft(
                title, selectedDate, note, countsAsMilestone, currentUrl1, currentUrl2,
                pickedPhoto1 = selectedImage1Uri != null,
                pickedPhoto2 = selectedImage2Uri != null
            ).hasChangesFrom(initialDraft)
            if (hasChanges) showDiscardPrompt = true else onDismiss()
        },
        title = {
            Text(
                text = stringResource(if (isNew) R.string.add_memory_dialog_title else R.string.edit_memory_dialog_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            val titleFocus = rememberInitialFocus(enabled = isNew)
            val noteFocus = remember { FocusRequester() }
            Column(
                modifier = Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ValidatedTextField(
                    field = InputField.MEMORY_TITLE,
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.memory_title_label),
                    placeholder = if (isNew) stringResource(R.string.memory_title_placeholder) else null,
                    modifier = Modifier.focusRequester(titleFocus),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { noteFocus.requestFocus() })
                )

                DateFieldButton(
                    label = stringResource(R.string.memory_date_label),
                    value = selectedDate.format(GermanLongDate),
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                )

                ValidatedTextField(
                    field = InputField.MEMORY_NOTE,
                    value = note,
                    onValueChange = { note = it },
                    label = stringResource(R.string.memory_note_label),
                    placeholder = if (isNew) stringResource(R.string.memory_note_placeholder) else null,
                    singleLine = false,
                    minLines = 3,
                    modifier = Modifier.focusRequester(noteFocus),
                    // Multi-line: Enter keeps inserting line breaks, so there is no Done action here.
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .toggleable(value = countsAsMilestone, role = Role.Checkbox) { checked ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            countsAsMilestone = checked
                        }
                ) {
                    Checkbox(
                        checked = countsAsMilestone,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                    )
                    Text(
                        text = stringResource(R.string.memory_counts_as_milestone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                PartnerPhotoPickerSlot(
                    partnerName = partner1Name,
                    partnerColor = partner1Color,
                    imageUri = selectedImage1Uri,
                    imageUrl = currentUrl1,
                    isEditable = editableRole == PartnerRole.PARTNER_1,
                    onPickImage = {
                        photoPicker1.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemoveImage = {
                        selectedImage1Uri = null
                        currentUrl1 = null
                    }
                )

                PartnerPhotoPickerSlot(
                    partnerName = partner2Name,
                    partnerColor = partner2Color,
                    imageUri = selectedImage2Uri,
                    imageUrl = currentUrl2,
                    isEditable = editableRole == PartnerRole.PARTNER_2,
                    onPickImage = {
                        photoPicker2.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemoveImage = {
                        selectedImage2Uri = null
                        currentUrl2 = null
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Only the current user's slot is reported; the partner's slot is never written
                    val ownPhoto = when (editableRole) {
                        PartnerRole.PARTNER_1 -> ownPhotoEdit(selectedImage1Uri, memory?.effectivePartner1Image, currentUrl1)
                        PartnerRole.PARTNER_2 -> ownPhotoEdit(selectedImage2Uri, memory?.effectivePartner2Image, currentUrl2)
                        null -> OwnPhotoEdit.Keep
                    }
                    val saved = (memory ?: Memory()).copy(
                        title = title.trim(),
                        date = selectedDate,
                        note = note.trim(),
                        countsAsMilestone = countsAsMilestone
                    )
                    onSave(saved, ownPhoto)
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(if (isNew) R.string.save_memory else R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (showDiscardPrompt) {
        DiscardChangesDialog(onDiscard = onDismiss, onKeepEditing = { showDiscardPrompt = false })
    }

    if (showDatePicker) {
        MemoryDatePickerDialog(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                selectedDate = date
            },
            // A moment has already happened
            selectableDates = remember { selectableDatesBetween(max = LocalDate.now()) }
        )
    }
}

private fun ownPhotoEdit(pickedUri: Uri?, storedUrl: String?, shownUrl: String?): OwnPhotoEdit = when {
    pickedUri != null -> OwnPhotoEdit.Replace(pickedUri)
    storedUrl != null && shownUrl == null -> OwnPhotoEdit.Remove
    else -> OwnPhotoEdit.Keep
}

@Composable
private fun PartnerPhotoPickerSlot(
    partnerName: String,
    partnerColor: Long,
    imageUri: Uri?,
    imageUrl: String?,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    modifier: Modifier = Modifier,
    isEditable: Boolean = true
) {
    val displayModel = imageUri ?: imageUrl
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                RoundedCornerShape(14.dp)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(partnerColor), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEditable) stringResource(R.string.photo_of_partner, partnerName)
                               else stringResource(R.string.photo_of_partner_readonly, partnerName),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isEditable && displayModel != null) {
                    TextButton(
                        onClick = onRemoveImage
                    ) {
                        Text(
                            text = stringResource(R.string.remove_photo),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (displayModel != null) {
                val previewContext = LocalContext.current
                val editPreviewRequest = remember(displayModel, previewContext) {
                    ImageRequest.Builder(previewContext)
                        .data(displayModel)
                        .crossfade(true)
                        .allowHardware(false)
                        .build()
                }
                AsyncImage(
                    model = editPreviewRequest,
                    placeholder = painterResource(R.drawable.ic_image_placeholder),
                    error = painterResource(R.drawable.ic_image_error),
                    contentDescription = stringResource(R.string.photo_of_partner, partnerName),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                if (isEditable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onPickImage,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.photo_selected_for_partner, partnerName),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            } else if (isEditable) {
                OutlinedButton(
                    onClick = onPickImage,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.select_photo_for_partner, partnerName),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.no_photo_of_partner_yet, partnerName),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.photo_only_by_partner, partnerName),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "MemoryFormDialog Light", showBackground = true)
@Composable
private fun MemoryFormDialogLightPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        MemoryFormDialog(
            memory = PreviewData.memory,
            partner1Name = PreviewData.space.partner1Name,
            partner2Name = PreviewData.space.partner2Name,
            partner1Color = 0xFFE65D2E,
            partner2Color = 0xFF5B8DB8,
            editableRole = PartnerRole.PARTNER_1,
            onDismiss = {},
            onSave = { _, _ -> }
        )
    }
}

@Preview(name = "MemoryFormDialog Dark", showBackground = true)
@Composable
private fun MemoryFormDialogDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        MemoryFormDialog(
            memory = null,
            partner1Name = PreviewData.space.partner1Name,
            partner2Name = PreviewData.space.partner2Name,
            partner1Color = 0xFFE65D2E,
            partner2Color = 0xFF5B8DB8,
            editableRole = PartnerRole.PARTNER_1,
            onDismiss = {},
            onSave = { _, _ -> }
        )
    }
}
