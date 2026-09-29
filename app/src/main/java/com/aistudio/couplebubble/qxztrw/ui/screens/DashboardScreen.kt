package com.aistudio.couplebubble.qxztrw.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import coil.compose.AsyncImage
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.ui.DashboardUiState
import com.aistudio.couplebubble.qxztrw.ui.theme.GoldenSunsetLight
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
    onAddMemory: (String, LocalDate, String, ByteArray?) -> Unit = { _, _, _, _ -> },
    onShowEditMemoryDialog: (Memory?) -> Unit = {},
    onShowDeleteMemoryDialog: (Memory?) -> Unit = {},
    onUpdateMemory: (Memory, ByteArray?) -> Unit = { _, _ -> },
    onDeleteMemory: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Du hast ${state.space.partnerBName} ein warmes Herz gesendet!")
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier.testTag("send_heart_fab")
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Herz senden",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
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

            Spacer(modifier = Modifier.height(32.dp))
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
                onDismiss = { onShowAddMemoryDialog(false) },
                onSave = { title, date, note, imageBytes ->
                    onAddMemory(title, date, note, imageBytes)
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
                onDismiss = { onShowEditMemoryDialog(null) },
                onSave = { updatedMemory, imageBytes ->
                    onUpdateMemory(updatedMemory, imageBytes)
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
    onAddMemoryClick: () -> Unit,
    onEditMemory: (Memory) -> Unit,
    onDeleteMemory: (Memory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.memory_timeline_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.memory_timeline_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

        Spacer(modifier = Modifier.height(8.dp))

        if (memories.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        RoundedCornerShape(20.dp)
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                memories.forEach { memory ->
                    MemoryCardItem(
                        memory = memory,
                        onEditMemory = onEditMemory,
                        onDeleteMemory = onDeleteMemory
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryCardItem(
    memory: Memory,
    onEditMemory: (Memory) -> Unit,
    onDeleteMemory: (Memory) -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN) }
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = memory.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = memory.date.format(formatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Erinnerungsoptionen",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
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
                }
            }

            if (memory.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = memory.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!memory.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = memory.imageUrl,
                    contentDescription = memory.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            RoundedCornerShape(14.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun LoveNoteCard(
    note: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.FormatQuote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .size(28.dp)
                    .padding(end = 12.dp)
            )
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
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
private fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, String, ByteArray?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf(LocalDate.now().toString()) }
    var note by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageUri = uri
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
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

                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Vorschau",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (selectedImageUri != null) stringResource(R.string.image_selected)
                        else stringResource(R.string.select_image)
                    )
                }
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
                        val imageBytes = selectedImageUri?.let { uri ->
                            try {
                                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            } catch (e: Exception) {
                                null
                            }
                        }
                        onSave(title.trim(), parsedDate, note.trim(), imageBytes)
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
    onDismiss: () -> Unit,
    onSave: (Memory, ByteArray?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(memory.title) }
    var dateString by remember { mutableStateOf(memory.date.toString()) }
    var note by remember { mutableStateOf(memory.note) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageUri = uri
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
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

                val displayImage = selectedImageUri ?: memory.imageUrl
                if (displayImage != null) {
                    AsyncImage(
                        model = displayImage,
                        contentDescription = "Vorschau",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (selectedImageUri != null || !memory.imageUrl.isNullOrBlank()) stringResource(R.string.image_selected)
                        else stringResource(R.string.select_image)
                    )
                }
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
                        val imageBytes = selectedImageUri?.let { uri ->
                            try {
                                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            } catch (e: Exception) {
                                null
                            }
                        }
                        val updated = memory.copy(
                            title = title.trim(),
                            date = parsedDate,
                            note = note.trim()
                        )
                        onSave(updated, imageBytes)
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
                        note = "Magischer Sommerurlaub am Meer."
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
                        note = "Magischer Sommerurlaub am Meer."
                    )
                )
            ),
            onMenuExpandedChanged = {},
            onShowDisconnectDialog = {},
            onConfirmDisconnect = {}
        )
    }
}
