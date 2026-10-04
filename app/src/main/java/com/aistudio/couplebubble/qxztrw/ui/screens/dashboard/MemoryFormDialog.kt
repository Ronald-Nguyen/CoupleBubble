package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.ui.GermanLongDate
import com.aistudio.couplebubble.qxztrw.ui.components.MemoryDatePickerDialog
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * Adds a new moment ([memory] = `null`) or edits [memory]. Each partner can only change their own photo slot;
 * the partner's slot is shown read-only and kept as it is on save.
 * [onSave] receives the memory without new photos plus the newly picked image of the current user's slot.
 */
@Composable
internal fun MemoryFormDialog(
    memory: Memory?,
    partner1Name: String,
    partner2Name: String,
    partner1Color: Long,
    partner2Color: Long,
    isCurrentUserPartner1: Boolean,
    onDismiss: () -> Unit,
    onSave: (memory: Memory, image1Uri: Uri?, image2Uri: Uri?) -> Unit
) {
    val isNew = memory == null
    var title by remember { mutableStateOf(memory?.title.orEmpty()) }
    var selectedDate by remember { mutableStateOf(memory?.date ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf(memory?.note.orEmpty()) }

    var selectedImage1Uri by remember { mutableStateOf<Uri?>(null) }
    var selectedImage2Uri by remember { mutableStateOf<Uri?>(null) }
    var currentUrl1 by remember { mutableStateOf(memory?.effectivePartner1Image) }
    var currentUrl2 by remember { mutableStateOf(memory?.effectivePartner2Image) }

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
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(if (isNew) R.string.add_memory_dialog_title else R.string.edit_memory_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.memory_title_label)) },
                    placeholder = if (isNew) {
                        { Text(stringResource(R.string.memory_title_placeholder)) }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedDate.format(GermanLongDate),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.memory_date_label)) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = stringResource(R.string.memory_date_label),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.memory_note_label)) },
                    placeholder = if (isNew) {
                        { Text(stringResource(R.string.memory_note_placeholder)) }
                    } else null,
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                PartnerPhotoPickerSlot(
                    partnerName = partner1Name,
                    partnerColor = partner1Color,
                    imageUri = selectedImage1Uri,
                    imageUrl = currentUrl1,
                    isEditable = isCurrentUserPartner1,
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
                    isEditable = !isCurrentUserPartner1,
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
                    if (title.isNotBlank()) {
                        // Only the current user's slot can change; the partner's slot keeps its stored photo
                        val image1Uri = selectedImage1Uri.takeIf { isCurrentUserPartner1 }
                        val image2Uri = selectedImage2Uri.takeIf { !isCurrentUserPartner1 }
                        val finalUrl1 = if (isCurrentUserPartner1) {
                            if (selectedImage1Uri != null) null else currentUrl1
                        } else {
                            memory?.effectivePartner1Image
                        }
                        val finalUrl2 = if (!isCurrentUserPartner1) {
                            if (selectedImage2Uri != null) null else currentUrl2
                        } else {
                            memory?.effectivePartner2Image
                        }
                        val saved = (memory ?: Memory()).copy(
                            title = title.trim(),
                            date = selectedDate,
                            note = note.trim(),
                            imageUrl = null,
                            partner1ImageUrl = finalUrl1,
                            partner2ImageUrl = finalUrl2
                        )
                        onSave(saved, image1Uri, image2Uri)
                        onDismiss()
                    }
                },
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

    if (showDatePicker) {
        MemoryDatePickerDialog(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { selectedDate = it }
        )
    }
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
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DeleteMemoryConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.delete_memory_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.delete_memory_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmDelete()
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(R.string.confirm_delete),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
