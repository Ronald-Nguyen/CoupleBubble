package com.aistudio.couplebubble.qxztrw.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SwapVert
import com.aistudio.couplebubble.qxztrw.ui.components.CustomColorPickerDialog
import com.aistudio.couplebubble.qxztrw.ui.components.ProfilePhotoCropDialog
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.data.LocalImageStorage
import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.ui.DashboardUiState
import com.aistudio.couplebubble.qxztrw.ui.theme.MyApplicationTheme
import com.aistudio.couplebubble.qxztrw.ui.theme.OceanBluePrimaryLight
import com.aistudio.couplebubble.qxztrw.ui.theme.SoftHeartPink
import com.aistudio.couplebubble.qxztrw.ui.theme.SunsetTerracottaLight
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun parseColorHexToCompose(hex: String?, fallbackHex: String = "#FF6B6B"): Color {
    val targetHex = if (!hex.isNullOrBlank()) hex.trim() else fallbackHex
    return try {
        Color(android.graphics.Color.parseColor(targetHex))
    } catch (e: Exception) {
        try {
            Color(android.graphics.Color.parseColor(fallbackHex))
        } catch (e2: Exception) {
            Color(0xFFE65D2E)
        }
    }
}

fun parseColorHexToLong(hex: String?, fallback: Long): Long {
    if (hex.isNullOrBlank()) return fallback
    return try {
        android.graphics.Color.parseColor(hex.trim()).toLong() and 0xFFFFFFFFL
    } catch (e: Exception) {
        fallback
    }
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onMenuExpandedChanged: (Boolean) -> Unit,
    onShowDisconnectDialog: (Boolean) -> Unit,
    onConfirmDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
    onShowEditNamesDialog: (Boolean) -> Unit = {},
    onUpdatePartnerNames: (String, String) -> Unit = { _, _ -> },
    onShowAddMemoryDialog: (Boolean) -> Unit = {},
    onAddMemory: (String, LocalDate, String, ByteArray?, ByteArray?) -> Unit = { _, _, _, _, _ -> },
    onShowEditMemoryDialog: (Memory?) -> Unit = {},
    onShowDeleteMemoryDialog: (Memory?) -> Unit = {},
    onUpdateMemory: (Memory, ByteArray?, ByteArray?) -> Unit = { _, _, _ -> },
    onDeleteMemory: (String) -> Unit = {},
    onShowGoogleBackupDialog: (Boolean) -> Unit = {},
    onSaveSpaceSetup: (String, String, LocalDate) -> Unit = { _, _, _ -> },
    onSignInWithGoogle: (String, String?, String?) -> Unit = { _, _, _ -> },
    onSignInWithGoogleClick: (Context) -> Unit = {},
    onSignOutGoogle: () -> Unit = {},
    onUploadProfilePhoto: (Boolean, Uri) -> Unit = { _, _ -> },
    onUploadProfilePhotoBytes: (Boolean, ByteArray) -> Unit = { _, _ -> },
    onUpdatePartnerColor: (String, Boolean) -> Unit = { _, _ -> },
    onSwapPartnerRoles: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Space Top Header with Avatar Ring Accents & Photo Placeholders
            SpaceTopHeader(
                space = state.space,
                isCurrentUserPartner1 = state.isCurrentUserPartner1,
                isUploadingProfilePhoto = state.isUploadingProfilePhoto,
                isMenuExpanded = state.isMenuExpanded,
                onMenuExpandedChanged = onMenuExpandedChanged,
                onPartnerAvatarDenied = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.only_own_avatar_editable))
                    }
                },
                onUploadProfilePhotoBytes = onUploadProfilePhotoBytes,
                onEditNamesClick = { onShowEditNamesDialog(true) },
                onGoogleBackupClick = { onShowGoogleBackupDialog(true) },
                onDisconnectClick = { onShowDisconnectDialog(true) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Hero Element: "Zusammen seit" Card with Ambient Glow
            HeroCounterCard(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Milestone Card: Next Anniversary with Progress
            MilestoneCard(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Expandable Memory Timeline Section Grouped By Year
            MemoryTimelineSection(
                memories = state.memories,
                partnerAName = state.space.partnerAName,
                partnerBName = state.space.partnerBName,
                partner1Color = parseColorHexToLong(state.space.partner1ColorHex, state.space.partner1AvatarColor),
                partner2Color = parseColorHexToLong(state.space.partner2ColorHex, state.space.partner2AvatarColor),
                onAddMemoryClick = { onShowAddMemoryDialog(true) },
                onEditMemory = onShowEditMemoryDialog,
                onDeleteMemory = onShowDeleteMemoryDialog,
                onImageClick = { imageUrl -> fullscreenImageUrl = imageUrl },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
            )

            Spacer(modifier = Modifier.height(84.dp))
        }

        // Fullscreen Image Dialog Modal
        fullscreenImageUrl?.let { imageUrl ->
            var isDownloading by remember { mutableStateOf(false) }
            var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
            var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }

            Dialog(
                onDismissRequest = {
                    fullscreenImageUrl = null
                    scale = 1f
                    offset = Offset.Zero
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.fullscreen_view),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                            .pointerInput(imageUrl) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        scale = if (scale > 1f) 1f else 2.5f
                                        offset = Offset.Zero
                                    }
                                )
                            }
                            .pointerInput(imageUrl) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                                    scale = newScale
                                    offset = if (newScale > 1f) {
                                        offset + pan
                                    } else {
                                        Offset.Zero
                                    }
                                }
                            }
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 40.dp, end = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (!isDownloading) {
                                    isDownloading = true
                                    scope.launch {
                                        val result = LocalImageStorage.saveImageToGallery(context, imageUrl)
                                        isDownloading = false
                                        if (result.isSuccess) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.photo_saved_to_gallery),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.photo_save_failed),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = stringResource(R.string.save_to_gallery),
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                fullscreenImageUrl = null
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Setup Space Dialog Modal if triggered
        if (state.showSetupSpaceDialog) {
            SpaceSetupDialog(
                currentPartnerA = state.space.partnerAName,
                currentPartnerB = state.space.partnerBName,
                initialDate = LocalDate.of(state.space.anniversaryYear, state.space.anniversaryMonth, state.space.anniversaryDay),
                onDismiss = { /* Non-dismissable if required, or close */ },
                onSave = onSaveSpaceSetup
            )
        }

        // Google Backup Dialog
        if (state.showGoogleBackupDialog) {
            val context = LocalContext.current
            AlertDialog(
                onDismissRequest = { onShowGoogleBackupDialog(false) },
                title = {
                    Text(
                        text = stringResource(R.string.google_backup_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    GoogleAuthCard(
                        userProfile = state.userProfile,
                        isLoading = state.isGoogleAuthLoading,
                        errorMessage = state.googleAuthError,
                        onSignInClick = {
                            onSignInWithGoogleClick(context)
                        },
                        onSignOutGoogle = {
                            onSignOutGoogle()
                            onShowGoogleBackupDialog(false)
                        },
                        onSignInWithGoogle = { uid, email, name ->
                            onSignInWithGoogle(uid, email, name)
                            onShowGoogleBackupDialog(false)
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.google_linked_success))
                            }
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onShowGoogleBackupDialog(false) }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // Disconnect Confirmation Dialog
        if (state.showDisconnectDialog) {
            AlertDialog(
                onDismissRequest = { onShowDisconnectDialog(false) },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.LinkOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.disconnect_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.disconnect_dialog_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = onConfirmDisconnect,
                        modifier = Modifier.testTag("confirm_disconnect_button")
                    ) {
                        Text(
                            text = stringResource(R.string.confirm_disconnect),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onShowDisconnectDialog(false) },
                        modifier = Modifier.testTag("cancel_disconnect_button")
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // Edit Names Dialog
        if (state.showEditNamesDialog) {
            EditNamesDialog(
                currentPartnerA = state.space.partnerAName,
                currentPartnerB = state.space.partnerBName,
                currentPartner1ColorHex = state.space.partner1ColorHex,
                currentPartner2ColorHex = state.space.partner2ColorHex,
                onDismiss = { onShowEditNamesDialog(false) },
                onSave = { newA, newB, newColor1, newColor2 ->
                    onUpdatePartnerNames(newA, newB)
                    if (newColor1 != state.space.partner1ColorHex) {
                        onUpdatePartnerColor(newColor1, true)
                    }
                    if (newColor2 != state.space.partner2ColorHex) {
                        onUpdatePartnerColor(newColor2, false)
                    }
                    onShowEditNamesDialog(false)
                    scope.launch {
                        snackbarHostState.showSnackbar("Spitznamen und Farben wurden aktualisiert")
                    }
                },
                onSwapRoles = {
                    onSwapPartnerRoles()
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.swap_partners_success))
                    }
                }
            )
        }

        // Add Memory Dialog
        if (state.showAddMemoryDialog) {
            AddMemoryDialog(
                partnerAName = state.space.partnerAName,
                partnerBName = state.space.partnerBName,
                partner1Color = parseColorHexToLong(state.space.partner1ColorHex, state.space.partner1AvatarColor),
                partner2Color = parseColorHexToLong(state.space.partner2ColorHex, state.space.partner2AvatarColor),
                isCurrentUserPartner1 = state.isCurrentUserPartner1,
                onDismiss = { onShowAddMemoryDialog(false) },
                onSave = { title, date, note, imageABytes, imageBBytes ->
                    onAddMemory(title, date, note, imageABytes, imageBBytes)
                    onShowAddMemoryDialog(false)
                    scope.launch {
                        snackbarHostState.showSnackbar("Neuer Moment festgehalten!")
                    }
                }
            )
        }

        // Edit Memory Dialog
        state.memoryToEdit?.let { memory ->
            EditMemoryDialog(
                memory = memory,
                partnerAName = state.space.partnerAName,
                partnerBName = state.space.partnerBName,
                partner1Color = parseColorHexToLong(state.space.partner1ColorHex, state.space.partner1AvatarColor),
                partner2Color = parseColorHexToLong(state.space.partner2ColorHex, state.space.partner2AvatarColor),
                isCurrentUserPartner1 = state.isCurrentUserPartner1,
                onDismiss = { onShowEditMemoryDialog(null) },
                onSave = { updatedMemory, imageABytes, imageBBytes ->
                    onUpdateMemory(updatedMemory, imageABytes, imageBBytes)
                    onShowEditMemoryDialog(null)
                    scope.launch {
                        snackbarHostState.showSnackbar("Erinnerung aktualisiert")
                    }
                }
            )
        }

        // Delete Memory Confirmation Dialog
        state.memoryToDelete?.let { memory ->
            DeleteMemoryConfirmationDialog(
                memory = memory,
                onDismiss = { onShowDeleteMemoryDialog(null) },
                onConfirmDelete = {
                    onDeleteMemory(memory.id)
                    onShowDeleteMemoryDialog(null)
                    scope.launch {
                        snackbarHostState.showSnackbar("Erinnerung gelöscht")
                    }
                }
            )
        }
    }
}

@Composable
private fun SpaceTopHeader(
    space: CoupleSpace,
    isCurrentUserPartner1: Boolean,
    isUploadingProfilePhoto: Boolean,
    isMenuExpanded: Boolean,
    onMenuExpandedChanged: (Boolean) -> Unit,
    onPartnerAvatarDenied: () -> Unit,
    onUploadProfilePhotoBytes: (Boolean, ByteArray) -> Unit = { _, _ -> },
    onEditNamesClick: () -> Unit,
    onGoogleBackupClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingCropUri = uri
        }
    }

    if (pendingCropUri != null) {
        ProfilePhotoCropDialog(
            imageUri = pendingCropUri,
            onDismiss = { pendingCropUri = null },
            onCropConfirmed = { bytes ->
                pendingCropUri = null
                onUploadProfilePhotoBytes(isCurrentUserPartner1, bytes)
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhotoAvatarWithRing(
                photoUrl = space.partner1PhotoUrl,
                initial = space.partner1Initial,
                ringColor = parseColorHexToCompose(space.partner1ColorHex, "#FF6B6B"),
                isLoading = isUploadingProfilePhoto && isCurrentUserPartner1,
                onClick = {
                    if (isCurrentUserPartner1) {
                        photoLauncher.launch("image/*")
                    } else {
                        onPartnerAvatarDenied()
                    }
                }
            )

            PulsingHeartConnector()

            PhotoAvatarWithRing(
                photoUrl = space.partner2PhotoUrl,
                initial = space.partner2Initial,
                ringColor = parseColorHexToCompose(space.partner2ColorHex, "#4ECDC4"),
                isLoading = isUploadingProfilePhoto && !isCurrentUserPartner1,
                onClick = {
                    if (!isCurrentUserPartner1) {
                        photoLauncher.launch("image/*")
                    } else {
                        onPartnerAvatarDenied()
                    }
                }
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "${space.partnerAName} & ${space.partnerBName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38B263))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.tagline),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box {
            IconButton(
                onClick = { onMenuExpandedChanged(true) },
                modifier = Modifier.testTag("settings_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { onMenuExpandedChanged(false) }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.edit_names),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        onMenuExpandedChanged(false)
                        onEditNamesClick()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.google_backup_title),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    onClick = {
                        onMenuExpandedChanged(false)
                        onGoogleBackupClick()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.disconnect_option),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.LinkOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        onMenuExpandedChanged(false)
                        onDisconnectClick()
                    },
                    modifier = Modifier.testTag("menu_disconnect_item")
                )
            }
        }
    }
}

@Composable
private fun PhotoAvatarWithRing(
    photoUrl: String? = null,
    initial: String,
    ringColor: Color,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .border(2.dp, ringColor.copy(alpha = 0.8f), CircleShape)
            .clickable(onClick = onClick)
            .padding(3.dp)
            .clip(CircleShape)
            .background(ringColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = ringColor,
                strokeWidth = 2.dp
            )
        } else if (!photoUrl.isNullOrBlank()) {
            var loadFailed by remember(photoUrl) { mutableStateOf(false) }
            if (loadFailed) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ringColor
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onError = { loadFailed = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        } else {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ringColor
            )
        }
    }
}

@Composable
private fun PulsingHeartConnector() {
    val infiniteTransition = rememberInfiniteTransition(label = "HeartPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HeartScale"
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = SoftHeartPink,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun HeroCounterCard(
    state: DashboardUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = OceanBluePrimaryLight.copy(alpha = 0.2f)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.together_since).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${state.metrics.totalDays}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 58.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.days_total).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                letterSpacing = 2.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "seit dem ${state.metrics.formattedStartDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 22.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CounterBreakdownPill(
                    value = "${state.metrics.years}",
                    label = stringResource(R.string.years),
                    modifier = Modifier.weight(1f)
                )

                CounterBreakdownPill(
                    value = "${state.metrics.months}",
                    label = stringResource(R.string.months),
                    modifier = Modifier.weight(1f)
                )

                CounterBreakdownPill(
                    value = "${state.metrics.days}",
                    label = stringResource(R.string.days),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CounterBreakdownPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            ),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MilestoneCard(
    state: DashboardUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = SunsetTerracottaLight.copy(alpha = 0.15f)
            )
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Celebration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.next_milestone).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = state.metrics.nextAnniversaryTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "In ${state.metrics.daysUntilNextAnniversary} Tagen",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Jahresfortschritt",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val percent = (state.metrics.progressToNextAnniversary * 100).toInt()
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { state.metrics.progressToNextAnniversary },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun MemoryTimelineSection(
    memories: List<Memory>,
    partnerAName: String,
    partnerBName: String,
    partner1Color: Long,
    partner2Color: Long,
    onAddMemoryClick: () -> Unit,
    onEditMemory: (Memory) -> Unit,
    onDeleteMemory: (Memory) -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedMemories = remember(memories) {
        memories.groupBy { it.date.year }.toSortedMap(compareByDescending { it })
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.memory_timeline_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            TextButton(
                onClick = onAddMemoryClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.add_memory),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (groupedMemories.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        RoundedCornerShape(22.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.no_memories_yet),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            groupedMemories.forEach { (year, yearMemories) ->
                YearMemoryGroupCard(
                    year = year,
                    memories = yearMemories,
                    partnerAName = partnerAName,
                    partnerBName = partnerBName,
                    partner1Color = partner1Color,
                    partner2Color = partner2Color,
                    onEditMemory = onEditMemory,
                    onDeleteMemory = onDeleteMemory,
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
    partnerAName: String,
    partnerBName: String,
    partner1Color: Long,
    partner2Color: Long,
    onEditMemory: (Memory) -> Unit,
    onDeleteMemory: (Memory) -> Unit,
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
                        text = "${memories.size} ${if (memories.size == 1) "Moment" else "Momente"}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Jahr $year ausklappen",
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
                            partnerAName = partnerAName,
                            partnerBName = partnerBName,
                            partner1Color = partner1Color,
                            partner2Color = partner2Color,
                            onEditMemory = onEditMemory,
                            onDeleteMemory = onDeleteMemory,
                            onImageClick = onImageClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCardItem(
    memory: Memory,
    partnerAName: String,
    partnerBName: String,
    partner1Color: Long,
    partner2Color: Long,
    onEditMemory: (Memory) -> Unit,
    onDeleteMemory: (Memory) -> Unit,
    onImageClick: (String) -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMAN) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
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
                    if (memory.hasAnyImage) {
                        val previewImage = memory.effectivePartnerAImage ?: memory.effectivePartnerBImage
                        if (!isCardExpanded && !previewImage.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(previewImage)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        RoundedCornerShape(6.dp)
                                    )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = if (memory.imageCount > 1) {
                                    stringResource(R.string.photo_count_badge_double)
                                } else {
                                    stringResource(R.string.photo_count_badge_single)
                                },
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            if (memory.imageCount > 1) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "2",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
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
                                        val urlToDownload = memory.effectivePartnerAImage ?: memory.effectivePartnerBImage
                                        if (urlToDownload != null) {
                                            scope.launch {
                                                val res = LocalImageStorage.saveImageToGallery(context, urlToDownload)
                                                if (res.isSuccess) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.photo_saved_to_gallery),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.photo_save_failed),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
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
                        contentDescription = "Details ausklappen",
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

                    val imageA = memory.effectivePartnerAImage
                    val imageB = memory.effectivePartnerBImage

                    if (!imageA.isNullOrBlank() || !imageB.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Partner A's image
                        if (!imageA.isNullOrBlank()) {
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
                                    text = stringResource(R.string.photo_of_partner, partnerAName),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageA)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = stringResource(R.string.photo_of_partner, partnerAName),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onImageClick(imageA) }
                            )
                        }

                        // Partner B's image (displayed UNDERNEATH Partner A's image)
                        if (!imageB.isNullOrBlank()) {
                            if (!imageA.isNullOrBlank()) {
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
                                    text = stringResource(R.string.photo_of_partner, partnerBName),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageB)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = stringResource(R.string.photo_of_partner, partnerBName),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onImageClick(imageB) }
                            )
                        }

                        // Prompt to add missing photo
                        if (imageA.isNullOrBlank() || imageB.isNullOrBlank()) {
                            val missingPartner = if (imageA.isNullOrBlank()) partnerAName else partnerBName
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

@Composable
private fun ColorPaletteSelector(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    partnerName: String = "",
    modifier: Modifier = Modifier
) {
    val palette = listOf(
        "#E65D2E" to R.string.color_terracotta,
        "#8FA89B" to R.string.color_sage,
        "#E8A598" to R.string.color_rose,
        "#4A7C92" to R.string.color_ocean,
        "#DDA15E" to R.string.color_amber,
        "#9A5865" to R.string.color_berry,
        "#4ECDC4" to R.string.color_mint,
        "#FF6B6B" to R.string.color_coral
    )

    var showCustomColorPicker by remember { mutableStateOf(false) }
    val isPresetSelected = palette.any { it.first.equals(selectedColorHex, ignoreCase = true) }

    if (showCustomColorPicker) {
        CustomColorPickerDialog(
            initialColorHex = selectedColorHex,
            partnerName = partnerName.ifBlank { "Partner" },
            onDismiss = { showCustomColorPicker = false },
            onColorSelected = { newHex ->
                onColorSelected(newHex)
                showCustomColorPicker = false
            }
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        palette.forEach { (hex, nameResId) ->
            val color = parseColorHexToCompose(hex)
            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(nameResId),
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Custom Color Picker Button
        val customColor = if (!isPresetSelected) parseColorHexToCompose(selectedColorHex) else MaterialTheme.colorScheme.surfaceContainerHigh
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(customColor)
                .border(
                    width = if (!isPresetSelected) 3.dp else 1.dp,
                    color = if (!isPresetSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    shape = CircleShape
                )
                .clickable { showCustomColorPicker = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = stringResource(R.string.custom_color_button),
                tint = if (!isPresetSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun EditNamesDialog(
    currentPartnerA: String,
    currentPartnerB: String,
    currentPartner1ColorHex: String,
    currentPartner2ColorHex: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
    onSwapRoles: () -> Unit
) {
    var nameA by remember { mutableStateOf(currentPartnerA) }
    var nameB by remember { mutableStateOf(currentPartnerB) }
    var color1 by remember { mutableStateOf(currentPartner1ColorHex) }
    var color2 by remember { mutableStateOf(currentPartner2ColorHex) }
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.edit_names),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Partner 1 Name & Color
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = nameA,
                        onValueChange = { nameA = it },
                        label = { Text(stringResource(R.string.partner_a_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.choose_accent_color) + " (${stringResource(R.string.partner_a_label)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ColorPaletteSelector(
                        selectedColorHex = color1,
                        partnerName = nameA,
                        onColorSelected = { color1 = it }
                    )
                }

                // Swap Roles Divider with Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val tempName = nameA
                            nameA = nameB
                            nameB = tempName

                            val tempColor = color1
                            color1 = color2
                            color2 = tempColor

                            onSwapRoles()
                        },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = stringResource(R.string.swap_partners_tooltip),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.swap_partners_tooltip),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }

                // Partner 2 Name & Color
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = nameB,
                        onValueChange = { nameB = it },
                        label = { Text(stringResource(R.string.partner_b_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.choose_accent_color) + " (${stringResource(R.string.partner_b_label)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ColorPaletteSelector(
                        selectedColorHex = color2,
                        partnerName = nameB,
                        onColorSelected = { color2 = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameA.isNotBlank() && nameB.isNotBlank()) {
                        onSave(nameA.trim(), nameB.trim(), color1, color2)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
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
                AsyncImage(
                    model = displayModel,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selected = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    onDateSelected(selected)
                }
                onDismiss()
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemoryDialog(
    partnerAName: String,
    partnerBName: String,
    partner1Color: Long,
    partner2Color: Long,
    isCurrentUserPartner1: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, String, ByteArray?, ByteArray?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMAN) }
    var selectedImageAUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerA = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageAUri = uri
    }

    val photoPickerB = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageBUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.add_memory_dialog_title),
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
                    placeholder = { Text(stringResource(R.string.memory_title_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedDate.format(dateFormatter),
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
                    placeholder = { Text(stringResource(R.string.memory_note_placeholder)) },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                PartnerPhotoPickerSlot(
                    partnerName = partnerAName,
                    partnerColor = partner1Color,
                    imageUri = selectedImageAUri,
                    imageUrl = null,
                    isEditable = isCurrentUserPartner1,
                    onPickImage = {
                        photoPickerA.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemoveImage = { selectedImageAUri = null }
                )

                PartnerPhotoPickerSlot(
                    partnerName = partnerBName,
                    partnerColor = partner2Color,
                    imageUri = selectedImageBUri,
                    imageUrl = null,
                    isEditable = !isCurrentUserPartner1,
                    onPickImage = {
                        photoPickerB.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemoveImage = { selectedImageBUri = null }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val imageABytes = if (isCurrentUserPartner1) {
                            selectedImageAUri?.let { uri ->
                                LocalImageStorage.compressImage(context, uri)
                            }
                        } else null
                        val imageBBytes = if (!isCurrentUserPartner1) {
                            selectedImageBUri?.let { uri ->
                                LocalImageStorage.compressImage(context, uri)
                            }
                        } else null
                        onSave(title.trim(), selectedDate, note.trim(), imageABytes, imageBBytes)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(R.string.save_memory))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMemoryDialog(
    memory: Memory,
    partnerAName: String,
    partnerBName: String,
    partner1Color: Long,
    partner2Color: Long,
    isCurrentUserPartner1: Boolean,
    onDismiss: () -> Unit,
    onSave: (Memory, ByteArray?, ByteArray?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(memory.title) }
    var selectedDate by remember { mutableStateOf(memory.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf(memory.note) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMAN) }

    var selectedImageAUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBUri by remember { mutableStateOf<Uri?>(null) }
    var currentUrlA by remember { mutableStateOf(memory.effectivePartnerAImage) }
    var currentUrlB by remember { mutableStateOf(memory.effectivePartnerBImage) }

    val photoPickerA = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageAUri = uri
    }

    val photoPickerB = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageBUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.edit_memory_dialog_title),
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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedDate.format(dateFormatter),
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
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                PartnerPhotoPickerSlot(
                    partnerName = partnerAName,
                    partnerColor = partner1Color,
                    imageUri = selectedImageAUri,
                    imageUrl = currentUrlA,
                    isEditable = isCurrentUserPartner1,
                    onPickImage = {
                        photoPickerA.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemoveImage = {
                        selectedImageAUri = null
                        currentUrlA = null
                    }
                )

                PartnerPhotoPickerSlot(
                    partnerName = partnerBName,
                    partnerColor = partner2Color,
                    imageUri = selectedImageBUri,
                    imageUrl = currentUrlB,
                    isEditable = !isCurrentUserPartner1,
                    onPickImage = {
                        photoPickerB.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemoveImage = {
                        selectedImageBUri = null
                        currentUrlB = null
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val imageABytes = if (isCurrentUserPartner1) {
                            selectedImageAUri?.let { uri ->
                                LocalImageStorage.compressImage(context, uri)
                            }
                        } else null
                        val imageBBytes = if (!isCurrentUserPartner1) {
                            selectedImageBUri?.let { uri ->
                                LocalImageStorage.compressImage(context, uri)
                            }
                        } else null

                        val finalUrlA = if (isCurrentUserPartner1) {
                            if (selectedImageAUri != null) null else currentUrlA
                        } else {
                            memory.effectivePartnerAImage
                        }

                        val finalUrlB = if (!isCurrentUserPartner1) {
                            if (selectedImageBUri != null) null else currentUrlB
                        } else {
                            memory.effectivePartnerBImage
                        }

                        val updated = memory.copy(
                            title = title.trim(),
                            date = selectedDate,
                            note = note.trim(),
                            imageUrl = finalUrlA ?: finalUrlB,
                            partnerAImageUrl = finalUrlA,
                            partnerBImageUrl = finalUrlB
                        )
                        onSave(updated, imageABytes, imageBBytes)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(R.string.save))
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
private fun DeleteMemoryConfirmationDialog(
    memory: Memory,
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

@Preview(name = "Dashboard Light", showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    MyApplicationTheme(darkTheme = false) {
        DashboardScreen(
            state = DashboardUiState(
                space = CoupleSpace(
                    partnerAName = "Alex",
                    partnerBName = "Sam",
                    anniversaryYear = 2025,
                    anniversaryMonth = 6,
                    anniversaryDay = 25
                ),
                metrics = RelationshipDateCalculator.calculate(2025, 6, 25),
                memories = listOf(
                    Memory(
                        id = "1",
                        title = "Erster Urlaub am Meer",
                        date = LocalDate.of(2025, 9, 18),
                        note = "Magischer Sommerurlaub am Meer. Zwei Perspektiven unseres Lieblingsmoments.",
                        partnerAImageUrl = "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2?w=800",
                        partnerBImageUrl = "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=800"
                    )
                )
            ),
            onMenuExpandedChanged = {},
            onShowDisconnectDialog = {},
            onConfirmDisconnect = {}
        )
    }
}

@Preview(name = "Dashboard Dark", showBackground = true)
@Composable
private fun DashboardScreenDarkPreview() {
    MyApplicationTheme(darkTheme = true) {
        DashboardScreen(
            state = DashboardUiState(
                space = CoupleSpace(
                    partnerAName = "Alex",
                    partnerBName = "Sam",
                    anniversaryYear = 2025,
                    anniversaryMonth = 6,
                    anniversaryDay = 25
                ),
                metrics = RelationshipDateCalculator.calculate(2025, 6, 25),
                memories = listOf(
                    Memory(
                        id = "1",
                        title = "Erster Urlaub am Meer",
                        date = LocalDate.of(2025, 9, 18),
                        note = "Magischer Sommerurlaub am Meer. Zwei Perspektiven unseres Lieblingsmoments.",
                        partnerAImageUrl = "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2?w=800",
                        partnerBImageUrl = "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=800"
                    )
                )
            ),
            onMenuExpandedChanged = {},
            onShowDisconnectDialog = {},
            onConfirmDisconnect = {}
        )
    }
}
