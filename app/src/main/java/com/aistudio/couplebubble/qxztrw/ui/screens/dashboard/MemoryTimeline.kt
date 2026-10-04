package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.ui.PhotoSlotKey
import com.aistudio.couplebubble.qxztrw.ui.PhotoSyncState
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import com.aistudio.couplebubble.qxztrw.ui.theme.OceanBluePrimaryLight
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun MemoryTimelineSection(
    memories: List<Memory>,
    partner1Name: String,
    partner2Name: String,
    partner1Color: Long,
    partner2Color: Long,
    isCurrentUserPartner1: Boolean,
    photoSyncStates: Map<PhotoSlotKey, PhotoSyncState>,
    onAddMemoryClick: () -> Unit,
    onEditMemory: (Memory) -> Unit,
    onDeleteMemory: (Memory) -> Unit,
    onSaveToGallery: (List<String>) -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val groupedMemories = remember(memories) {
        memories.groupBy { it.date.year }.toSortedMap(compareByDescending { it })
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (groupedMemories.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                        RoundedCornerShape(22.dp)
                    )
                    .testTag("empty_timeline_card")
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
                            onAddMemoryClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.testTag("add_first_memory_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.add_first_memory),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            groupedMemories.forEach { (year, yearMemories) ->
                YearMemoryGroupCard(
                    year = year,
                    memories = yearMemories,
                    partner1Name = partner1Name,
                    partner2Name = partner2Name,
                    partner1Color = partner1Color,
                    partner2Color = partner2Color,
                    isCurrentUserPartner1 = isCurrentUserPartner1,
                    photoSyncStates = photoSyncStates,
                    onEditMemory = onEditMemory,
                    onDeleteMemory = onDeleteMemory,
                    onSaveToGallery = onSaveToGallery,
                    onImageClick = onImageClick
                )
            }
        }
    }
}

@Composable
private fun YearMemoryGroupCard(
    year: Int,
    memories: List<Memory>,
    partner1Name: String,
    partner2Name: String,
    partner1Color: Long,
    partner2Color: Long,
    isCurrentUserPartner1: Boolean,
    photoSyncStates: Map<PhotoSlotKey, PhotoSyncState>,
    onEditMemory: (Memory) -> Unit,
    onDeleteMemory: (Memory) -> Unit,
    onSaveToGallery: (List<String>) -> Unit,
    onImageClick: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = OceanBluePrimaryLight.copy(alpha = 0.15f)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "$year",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = pluralStringResource(R.plurals.memory_count, memories.size, memories.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.expand_year, year),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    memories.forEach { memory ->
                        MemoryCardItem(
                            memory = memory,
                            partner1Name = partner1Name,
                            partner2Name = partner2Name,
                            partner1Color = partner1Color,
                            partner2Color = partner2Color,
                            isCurrentUserPartner1 = isCurrentUserPartner1,
                            syncState1 = photoSyncStates[PhotoSlotKey(memory.id, isPartner1 = true)],
                            syncState2 = photoSyncStates[PhotoSlotKey(memory.id, isPartner1 = false)],
                            onEditMemory = onEditMemory,
                            onDeleteMemory = onDeleteMemory,
                            onSaveToGallery = onSaveToGallery,
                            onImageClick = onImageClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryPhotoSlot(
    imageUrl: String,
    partnerName: String,
    borderColor: Color,
    isLocked: Boolean,
    syncState: PhotoSyncState?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val request = remember(imageUrl, context) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .allowHardware(false)
            .build()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = request,
            placeholder = painterResource(R.drawable.ic_image_placeholder),
            error = painterResource(R.drawable.ic_image_error),
            contentDescription = stringResource(R.string.photo_of_partner, partnerName),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (isLocked) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = stringResource(R.string.photo_slot_locked, partnerName),
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(3.dp)
                    .size(14.dp)
            )
        }
        PhotoSyncBadge(
            syncState = syncState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        )
    }
}

/** Small corner badge: spinner + cloud while uploading, then a brief check mark before it fades out. */

@Composable
private fun PhotoSyncBadge(
    syncState: PhotoSyncState?,
    modifier: Modifier = Modifier
) {
    // Keeps the last state visible while the badge animates out
    var displayedState by remember { mutableStateOf(syncState ?: PhotoSyncState.UPLOADING) }
    LaunchedEffect(syncState) {
        if (syncState != null) displayedState = syncState
    }

    AnimatedVisibility(
        visible = syncState != null,
        enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
            scaleIn(spring(stiffness = Spring.StiffnessLow), initialScale = 0.8f),
        exit = fadeOut(spring(stiffness = Spring.StiffnessVeryLow)) +
            scaleOut(spring(stiffness = Spring.StiffnessLow), targetScale = 0.9f),
        modifier = modifier
    ) {
        val description = stringResource(
            if (displayedState == PhotoSyncState.UPLOADING) R.string.photo_sync_uploading else R.string.photo_sync_done
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .semantics { contentDescription = description }
        ) {
            AnimatedContent(
                targetState = displayedState,
                transitionSpec = {
                    (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        scaleIn(spring(stiffness = Spring.StiffnessMediumLow), initialScale = 0.6f))
                        .togetherWith(fadeOut(spring(stiffness = Spring.StiffnessMediumLow)))
                },
                label = "photoSyncState"
            ) { state ->
                when (state) {
                    PhotoSyncState.UPLOADING -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Color.White.copy(alpha = 0.9f),
                            strokeWidth = 1.5.dp,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    PhotoSyncState.SYNCED -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryPreviewThumbnail(
    imageUrl: String,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val previewRequest = remember(imageUrl, context) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .allowHardware(false)
            .build()
    }
    AsyncImage(
        model = previewRequest,
        placeholder = painterResource(R.drawable.ic_image_placeholder),
        error = painterResource(R.drawable.ic_image_error),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(6.dp))
    )
}

@Composable
private fun MemoryCardItem(
    memory: Memory,
    partner1Name: String,
    partner2Name: String,
    partner1Color: Long,
    partner2Color: Long,
    isCurrentUserPartner1: Boolean,
    syncState1: PhotoSyncState?,
    syncState2: PhotoSyncState?,
    onEditMemory: (Memory) -> Unit,
    onDeleteMemory: (Memory) -> Unit,
    onSaveToGallery: (List<String>) -> Unit,
    onImageClick: (String) -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMAN) }
    var menuExpanded by remember { mutableStateOf(false) }
    var isCardExpanded by remember { mutableStateOf(true) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { isCardExpanded = !isCardExpanded }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = memory.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = memory.date.format(formatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // One thumbnail per photo, framed in its owner's accent color like the photo slots
                    if (!isCardExpanded) {
                        listOf(
                            memory.effectivePartner1Image to partner1Color,
                            memory.effectivePartner2Image to partner2Color
                        ).forEach { (previewImage, ownerColor) ->
                            if (!previewImage.isNullOrBlank()) {
                                MemoryPreviewThumbnail(
                                    imageUrl = previewImage,
                                    borderColor = Color(ownerColor),
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Erinnerungsoptionen",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (memory.hasAnyImage) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.save_to_gallery)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        val urlsToDownload = listOfNotNull(
                                            memory.effectivePartner1Image,
                                            memory.effectivePartner2Image
                                        ).filter { it.isNotBlank() }.distinct()
                                        onSaveToGallery(urlsToDownload)
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit_memory)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEditMemory(memory)
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.delete_memory),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteMemory(memory)
                                }
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isCardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.expand_memory_details),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isCardExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    if (memory.note.isNotBlank()) {
                        Text(
                            text = memory.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }

                    val image1 = memory.effectivePartner1Image
                    val image2 = memory.effectivePartner2Image

                    if (!image1.isNullOrBlank() || !image2.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Partner A's image
                        if (!image1.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(partner1Color), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.photo_of_partner, partner1Name),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            MemoryPhotoSlot(
                                imageUrl = image1,
                                partnerName = partner1Name,
                                borderColor = Color(partner1Color),
                                isLocked = !isCurrentUserPartner1,
                                syncState = syncState1,
                                onClick = { onImageClick(image1) }
                            )
                        }

                        // Partner B's image (displayed UNDERNEATH Partner A's image)
                        if (!image2.isNullOrBlank()) {
                            if (!image1.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(partner2Color), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.photo_of_partner, partner2Name),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            MemoryPhotoSlot(
                                imageUrl = image2,
                                partnerName = partner2Name,
                                borderColor = Color(partner2Color),
                                isLocked = isCurrentUserPartner1,
                                syncState = syncState2,
                                onClick = { onImageClick(image2) }
                            )
                        }

                        // Prompt to add missing photo
                        if (image1.isNullOrBlank() || image2.isNullOrBlank()) {
                            val missingPartner = if (image1.isNullOrBlank()) partner1Name else partner2Name
                            OutlinedButton(
                                onClick = { onEditMemory(memory) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.add_partner_photo_prompt, missingPartner),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Photo Sync Badge Light", showBackground = true)
@Composable
private fun PhotoSyncBadgePreview() {
    CoupleBubbleTheme(darkTheme = false) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp)
        ) {
            PhotoSyncBadge(syncState = PhotoSyncState.UPLOADING)
            PhotoSyncBadge(syncState = PhotoSyncState.SYNCED)
        }
    }
}

@Preview(name = "Photo Sync Badge Dark", showBackground = true)
@Composable
private fun PhotoSyncBadgeDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp)
        ) {
            PhotoSyncBadge(syncState = PhotoSyncState.UPLOADING)
            PhotoSyncBadge(syncState = PhotoSyncState.SYNCED)
        }
    }
}
