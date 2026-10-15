package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import com.aistudio.couplebubble.qxztrw.ui.components.LocalReducedMotion
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistudio.couplebubble.qxztrw.ui.components.circularTouchTarget
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.SpaceDefaults
import com.aistudio.couplebubble.qxztrw.domain.model.photoOf
import com.aistudio.couplebubble.qxztrw.ui.components.ProfilePhotoCropDialog
import com.aistudio.couplebubble.qxztrw.ui.preview.PreviewData
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import com.aistudio.couplebubble.qxztrw.ui.theme.SoftHeartPink
import com.aistudio.couplebubble.qxztrw.ui.theme.getContrastingTextColor
import com.aistudio.couplebubble.qxztrw.ui.theme.parseColorHexToCompose
import kotlinx.coroutines.launch

@Composable
internal fun SpaceTopHeader(
    space: CoupleSpace,
    editableRole: PartnerRole?,
    isUploadingProfilePhoto: Boolean,
    isMenuExpanded: Boolean,
    onMenuExpandedChanged: (Boolean) -> Unit,
    onPartnerAvatarDenied: () -> Unit,
    onProfileNotLinked: () -> Unit = {},
    onUploadProfilePhotoBytes: (ByteArray) -> Unit = {},
    onViewProfilePhoto: (String) -> Unit = {},
    onEditNamesClick: () -> Unit = {},
    onGoogleBackupClick: () -> Unit = {},
    onMilestonesClick: () -> Unit = {},
    onReunionClick: () -> Unit = {},
    onDisconnectClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    isSearchOpen: Boolean = false,
    searchQuery: String = "",
    onOpenSearch: () -> Unit = {},
    onCloseSearch: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {}
) {
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val shareMessage = stringResource(R.string.share_code_message, space.formattedPairingCode)
    val shareChooserTitle = stringResource(R.string.share_code_chooser)

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
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
                onUploadProfilePhotoBytes(bytes)
            }
        )
    }

    var avatarMenuRole by remember { mutableStateOf<PartnerRole?>(null) }

    // The slot is resolved again in the repository, so this only decides which avatar opens the picker
    val onAvatarClick: (PartnerRole, String?) -> Unit = { slot, photoUrl ->
        val hasPhoto = !photoUrl.isNullOrBlank()
        when {
            editableRole == slot && hasPhoto -> avatarMenuRole = slot
            editableRole == slot -> photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            hasPhoto -> onViewProfilePhoto(photoUrl.orEmpty())
            editableRole == null -> onProfileNotLinked()
            else -> onPartnerAvatarDenied()
        }
    }

    @Composable
    fun OwnAvatarMenu(role: PartnerRole, photoUrl: String?) {
        DropdownMenu(
            expanded = avatarMenuRole == role,
            onDismissRequest = { avatarMenuRole = null }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_photo_view)) },
                leadingIcon = { Icon(Icons.Default.ZoomIn, contentDescription = null) },
                onClick = {
                    avatarMenuRole = null
                    photoUrl?.takeIf { it.isNotBlank() }?.let(onViewProfilePhoto)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_photo_change)) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    avatarMenuRole = null
                    photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )
        }
    }

    @Composable
    fun PartnerAvatar(role: PartnerRole) {
        val isPartner1 = role == PartnerRole.PARTNER_1
        val photoUrl = space.photoOf(role)
        val hasPhoto = !photoUrl.isNullOrBlank()
        val clickLabel = when {
            editableRole == role && hasPhoto -> stringResource(R.string.avatar_action_options)
            editableRole == role -> stringResource(R.string.avatar_action_pick)
            hasPhoto -> stringResource(R.string.avatar_action_view)
            else -> null
        }
        Box {
            PhotoAvatarWithRing(
                description = stringResource(
                    R.string.profile_photo_of,
                    if (isPartner1) space.partner1Name else space.partner2Name
                ),
                clickLabel = clickLabel,
                photoUrl = photoUrl,
                initial = if (isPartner1) space.partner1Initial else space.partner2Initial,
                ringColor = if (isPartner1) {
                    parseColorHexToCompose(space.partner1ColorHex, SpaceDefaults.PARTNER_1_COLOR_HEX)
                } else {
                    parseColorHexToCompose(space.partner2ColorHex, SpaceDefaults.PARTNER_2_COLOR_HEX)
                },
                isLoading = isUploadingProfilePhoto && editableRole == role,
                onClick = { onAvatarClick(role, photoUrl) }
            )
            OwnAvatarMenu(role, photoUrl)
        }
    }

    if (isSearchOpen) {
        val haptic = LocalHapticFeedback.current
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("memories_search_bar")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCloseSearch()
                    },
                    modifier = Modifier.testTag("memories_search_close")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.memories_search_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                // The field keeps its own text: the query in the UI state comes back from a background
                // computation, and feeding it back while typing fast would reorder or drop characters
                var text by rememberSaveable { mutableStateOf(searchQuery) }
                val searchFieldLabel = stringResource(R.string.memories_search_placeholder)
                BasicTextField(
                    value = text,
                    onValueChange = { text = it; onSearchQueryChange(it) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (text.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.memories_search_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = searchFieldLabel }
                        .testTag("memories_search_input")
                )

                if (text.isNotEmpty()) {
                    IconButton(
                        onClick = { text = ""; onSearchQueryChange("") },
                        modifier = Modifier.testTag("memories_search_clear")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.memories_search_clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PartnerAvatar(PartnerRole.PARTNER_1)
                PulsingHeartConnector()
                PartnerAvatar(PartnerRole.PARTNER_2)

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "${space.partner1Name} & ${space.partner2Name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.tagline),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val haptic = LocalHapticFeedback.current
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenSearch()
                    },
                    modifier = Modifier.testTag("memories_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.memories_search_hint),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(
                        onClick = { onMenuExpandedChanged(true) },
                        modifier = Modifier.testTag("settings_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_options),
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
                            text = stringResource(R.string.reunion_menu),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.EventAvailable,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    onClick = {
                        onMenuExpandedChanged(false)
                        onReunionClick()
                    },
                    modifier = Modifier.testTag("menu_reunion_item")
                )

                if (space.pairingCode.length == 6) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.share_room_code),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        onClick = {
                            onMenuExpandedChanged(false)
                            shareRoomCode(context, shareMessage, shareChooserTitle)
                        },
                        modifier = Modifier.testTag("menu_share_code_item")
                    )
                }

                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.settings),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    onClick = {
                        onMenuExpandedChanged(false)
                        onSettingsClick()
                    },
                    modifier = Modifier.testTag("menu_settings_item")
                )
            }
        }
    }
    }
}
}

@Composable
private fun PhotoAvatarWithRing(
    photoUrl: String? = null,
    initial: String,
    ringColor: Color,
    description: String,
    clickLabel: String?,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .semantics { contentDescription = description }
            .circularTouchTarget(visualSize = 46.dp, onClickLabel = clickLabel, onClick = onClick)
            .border(2.dp, ringColor.copy(alpha = 0.8f), CircleShape)
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
                AvatarInitial(initial = initial, fillColor = ringColor)
            } else {
                val avatarContext = LocalContext.current
                val avatarRequest = remember(photoUrl, avatarContext) {
                    ImageRequest.Builder(avatarContext)
                        .data(photoUrl)
                        .crossfade(true)
                        .allowHardware(false)
                        .build()
                }
                AsyncImage(
                    model = avatarRequest,
                    placeholder = painterResource(R.drawable.ic_image_placeholder),
                    error = painterResource(R.drawable.ic_image_error),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onError = { loadFailed = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        } else {
            AvatarInitial(initial = initial, fillColor = ringColor)
        }
    }
}

@Composable
private fun AvatarInitial(initial: String, fillColor: Color) {
    val textColor = remember(fillColor) { getContrastingTextColor(fillColor) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fillColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun PulsingHeartConnector() {
    // The scale is read inside graphicsLayer, so each frame only redraws instead of recomposing
    val scale: State<Float> = if (LocalReducedMotion.current) {
        remember { mutableFloatStateOf(1f) }
    } else {
        rememberInfiniteTransition(label = "HeartPulse").animateFloat(
            initialValue = 0.9f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "HeartScale"
        )
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
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

@Preview(name = "SpaceTopHeader Light", showBackground = true)
@Composable
private fun SpaceTopHeaderLightPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        SpaceTopHeader(
            space = PreviewData.space,
            editableRole = PartnerRole.PARTNER_1,
            isUploadingProfilePhoto = false,
            isMenuExpanded = false,
            onMenuExpandedChanged = {},
            onPartnerAvatarDenied = {},
            onEditNamesClick = {},
            onGoogleBackupClick = {},
            onMilestonesClick = {},
            onDisconnectClick = {}
        )
    }
}

@Preview(name = "SpaceTopHeader Dark", showBackground = true)
@Composable
private fun SpaceTopHeaderDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        SpaceTopHeader(
            space = PreviewData.space,
            editableRole = PartnerRole.PARTNER_1,
            isUploadingProfilePhoto = true,
            isMenuExpanded = false,
            onMenuExpandedChanged = {},
            onPartnerAvatarDenied = {},
            onEditNamesClick = {},
            onGoogleBackupClick = {},
            onMilestonesClick = {},
            onDisconnectClick = {}
        )
    }
}
