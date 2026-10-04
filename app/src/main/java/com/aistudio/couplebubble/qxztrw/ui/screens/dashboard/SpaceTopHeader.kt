package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.SpaceDefaults
import com.aistudio.couplebubble.qxztrw.ui.components.ProfilePhotoCropDialog
import com.aistudio.couplebubble.qxztrw.ui.theme.SoftHeartPink
import com.aistudio.couplebubble.qxztrw.ui.theme.getContrastingTextColor
import com.aistudio.couplebubble.qxztrw.ui.theme.parseColorHexToCompose
import kotlinx.coroutines.launch

@Composable
internal fun SpaceTopHeader(
    space: CoupleSpace,
    isCurrentUserPartner1: Boolean,
    isUploadingProfilePhoto: Boolean,
    isMenuExpanded: Boolean,
    onMenuExpandedChanged: (Boolean) -> Unit,
    onPartnerAvatarDenied: () -> Unit,
    onUploadProfilePhotoBytes: (Boolean, ByteArray) -> Unit = { _, _ -> },
    onEditNamesClick: () -> Unit,
    onGoogleBackupClick: () -> Unit,
    onMilestonesClick: () -> Unit,
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
                ringColor = parseColorHexToCompose(space.partner1ColorHex, SpaceDefaults.PARTNER_1_COLOR_HEX),
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
                ringColor = parseColorHexToCompose(space.partner2ColorHex, SpaceDefaults.PARTNER_2_COLOR_HEX),
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
                    text = "${space.partner1Name} & ${space.partner2Name}",
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
                            text = stringResource(R.string.milestone_settings_title),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    onClick = {
                        onMenuExpandedChanged(false)
                        onMilestonesClick()
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
