package com.aistudio.couplebubble.qxztrw.ui.screens

import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    onSignOutGoogle: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
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
                isMenuExpanded = state.isMenuExpanded,
                onMenuExpandedChanged = onMenuExpandedChanged,
                onAvatarClick = { partnerName ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Foto-Upload für $partnerName ausgewählt")
                    }
                },
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
                partner1Color = state.space.partner1AvatarColor,
                partner2Color = state.space.partner2AvatarColor,
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
        if (fullscreenImageUrl != null) {
            Dialog(
                onDismissRequest = { fullscreenImageUrl = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { fullscreenImageUrl = null },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = fullscreenImageUrl,
                        contentDescription = "Vollbildansicht",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )

                    IconButton(
                        onClick = { fullscreenImageUrl = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 40.dp, end = 20.dp)
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Schließen",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
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
                                snackbarHostState.showSnackbar("Google-Datensicherung aktiviert")
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
                onDismiss = { onShowEditNamesDialog(false) },
                onSave = { newA, newB ->
                    onUpdatePartnerNames(newA, newB)
                    onShowEditNamesDialog(false)
                    scope.launch {
                        snackbarHostState.showSnackbar("Kosenamen wurden aktualisiert")
                    }
                }
            )
        }

        // Add Memory Dialog
        if (state.showAddMemoryDialog) {
            AddMemoryDialog(
                partnerAName = state.space.partnerAName,
                partnerBName = state.space.partnerBName,
                partner1Color = state.space.partner1AvatarColor,
                partner2Color = state.space.partner2AvatarColor,
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
                partner1Color = state.space.partner1AvatarColor,
                partner2Color = state.space.partner2AvatarColor,
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
    isMenuExpanded: Boolean,
    onMenuExpandedChanged: (Boolean) -> Unit,
    onAvatarClick: (String) -> Unit,
    onEditNamesClick: () -> Unit,
    onGoogleBackupClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
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
                initial = space.partner1Initial,
                ringColor = MaterialTheme.colorScheme.primary,
                onClick = { onAvatarClick(space.partnerAName) }
            )

            PulsingHeartConnector()

            PhotoAvatarWithRing(
                initial = space.partner2Initial,
                ringColor = MaterialTheme.colorScheme.secondary,
                onClick = { onAvatarClick(space.partnerBName) }
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
    initial: String,
    ringColor: Color,
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
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ringColor
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
    var menuExpanded by remember { mutableStateOf(false) }
    var isCardExpanded by remember { mutableStateOf(false) }

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
                                model = imageA,
                                contentDescription = stringResource(R.string.photo_of_partner, partnerAName),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(14.dp))
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
                                model = imageB,
                                contentDescription = stringResource(R.string.photo_of_partner, partnerBName),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(14.dp))
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
private fun EditNamesDialog(
    currentPartnerA: String,
    currentPartnerB: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var nameA by remember { mutableStateOf(currentPartnerA) }
    var nameB by remember { mutableStateOf(currentPartnerB) }

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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameA.isNotBlank() && nameB.isNotBlank()) {
                        onSave(nameA.trim(), nameB.trim())
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
    modifier: Modifier = Modifier
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
                        text = stringResource(R.string.photo_of_partner, partnerName),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (displayModel != null) {
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
            } else {
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
            }
        }
    }
}

@Composable
private fun AddMemoryDialog(
    partnerAName: String,
    partnerBName: String,
    partner1Color: Long,
    partner2Color: Long,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, String, ByteArray?, ByteArray?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf(LocalDate.now().toString()) }
    var note by remember { mutableStateOf("") }
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

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text(stringResource(R.string.memory_date_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

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
                        val parsedDate = try {
                            LocalDate.parse(dateString.trim())
                        } catch (e: Exception) {
                            LocalDate.now()
                        }
                        val imageABytes = selectedImageAUri?.let { uri ->
                            LocalImageStorage.compressImage(context, uri)
                        }
                        val imageBBytes = selectedImageBUri?.let { uri ->
                            LocalImageStorage.compressImage(context, uri)
                        }
                        onSave(title.trim(), parsedDate, note.trim(), imageABytes, imageBBytes)
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
}

@Composable
private fun EditMemoryDialog(
    memory: Memory,
    partnerAName: String,
    partnerBName: String,
    partner1Color: Long,
    partner2Color: Long,
    onDismiss: () -> Unit,
    onSave: (Memory, ByteArray?, ByteArray?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(memory.title) }
    var dateString by remember { mutableStateOf(memory.date.toString()) }
    var note by remember { mutableStateOf(memory.note) }

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

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text(stringResource(R.string.memory_date_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

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
                        val parsedDate = try {
                            LocalDate.parse(dateString.trim())
                        } catch (e: Exception) {
                            memory.date
                        }
                        val imageABytes = selectedImageAUri?.let { uri ->
                            LocalImageStorage.compressImage(context, uri)
                        }
                        val imageBBytes = selectedImageBUri?.let { uri ->
                            LocalImageStorage.compressImage(context, uri)
                        }
                        val finalUrlA = if (selectedImageAUri != null) null else currentUrlA
                        val finalUrlB = if (selectedImageBUri != null) null else currentUrlB

                        val updated = memory.copy(
                            title = title.trim(),
                            date = parsedDate,
                            note = note.trim(),
                            imageUrl = finalUrlA,
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
