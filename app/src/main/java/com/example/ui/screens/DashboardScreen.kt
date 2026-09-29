package com.example.ui.screens

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CoupleSpace
import com.example.model.MemoryMilestone
import com.example.model.RelationshipDateCalculator
import com.example.ui.DashboardUiState
import com.example.ui.theme.GoldenSunsetLight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OceanBluePrimaryLight
import com.example.ui.theme.SoftHeartPink
import com.example.ui.theme.SunsetTerracottaLight
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onMenuExpandedChanged: (Boolean) -> Unit,
    onShowDisconnectDialog: (Boolean) -> Unit,
    onConfirmDisconnect: () -> Unit,
    modifier: Modifier = Modifier
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
                        snackbarHostState.showSnackbar("Du hast ${state.space.partner2Name} ein warmes Herz gesendet!")
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
                        snackbarHostState.showSnackbar("Foto-Upload für $partnerName ausgewählt (Kamera/Galerie)")
                    }
                },
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

            Spacer(modifier = Modifier.height(22.dp))

            // Mini Memory Milestones Timeline
            MemoryMilestonesSection(
                memories = state.space.memories,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Daily Thought / Intimate Love Note Card
            LoveNoteCard(
                note = state.loveNoteText,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
            )

            Spacer(modifier = Modifier.height(84.dp)) // Padding for FAB
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
                        onClick = { onShowDisconnectDialog(false) }
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface
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
    onDisconnectClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Connected Avatars with Ring Accents & Photo Placeholders
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhotoAvatarWithRing(
                initial = space.partner1Initial,
                ringColor = MaterialTheme.colorScheme.primary,
                onClick = { onAvatarClick(space.partner1Name) }
            )

            PulsingHeartConnector()

            PhotoAvatarWithRing(
                initial = space.partner2Initial,
                ringColor = MaterialTheme.colorScheme.secondary,
                onClick = { onAvatarClick(space.partner2Name) }
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
                        text = "Gemeinsamer Raum",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Settings Dropdown Menu
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
            .size(48.dp)
            .clip(CircleShape)
            .border(width = 2.5.dp, color = ringColor, shape = CircleShape)
            .padding(3.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        ringColor,
                        ringColor.copy(alpha = 0.85f)
                    )
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        // Subtle photo add overlay icon badge in corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(14.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = "Foto hochladen",
                tint = ringColor,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

@Composable
private fun PulsingHeartConnector() {
    val infiniteTransition = rememberInfiniteTransition(label = "heartBeat")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = SoftHeartPink,
            modifier = Modifier
                .size(18.dp)
                .scale(scale)
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
                elevation = 10.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = SunsetTerracottaLight.copy(alpha = 0.3f)
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        OceanBluePrimaryLight.copy(alpha = 0.3f),
                        SunsetTerracottaLight.copy(alpha = 0.45f)
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            ),
        shape = RoundedCornerShape(26.dp),
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
            // Header Pill: "Zusammen seit [Datum]"
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.padding(bottom = 18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${stringResource(R.string.together_since)} ${state.metrics.formattedStartDate}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Big Total Days Hero Metric
            Text(
                text = "${state.metrics.totalDays}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-1).sp
            )

            Text(
                text = "Tage voller Liebe",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 22.dp)
            )

            // Breakdown Grid: Jahre - Monate - Tage
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
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Celebration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.next_milestone),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = state.metrics.nextAnniversaryTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Days until pill
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

            // Milestone Progress Bar with Terracotta to Sunset Gradient
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
private fun MemoryMilestonesSection(
    memories: List<MemoryMilestone>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Unsere Meilensteine & Erinnerungen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Vertical Timeline Items
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            memories.forEach { milestone ->
                MemoryMilestoneItem(milestone = milestone)
            }
        }
    }
}

@Composable
private fun MemoryMilestoneItem(
    milestone: MemoryMilestone
) {
    val icon: ImageVector = when (milestone.iconType) {
        "FIRST_DATE" -> Icons.Default.Favorite
        "TRIP" -> Icons.Default.Explore
        "HOME" -> Icons.Default.Home
        else -> Icons.Outlined.Celebration
    }

    val iconColor = when (milestone.iconType) {
        "FIRST_DATE" -> SunsetTerracottaLight
        "TRIP" -> OceanBluePrimaryLight
        "HOME" -> GoldenSunsetLight
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = milestone.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = milestone.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = milestone.dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )
        }
    }
}

// -------------------------------------------------------------
// Previews with Mock Data
// -------------------------------------------------------------

@Preview(name = "Dashboard - Light", showBackground = true)
@Composable
private fun DashboardScreenLightPreview() {
    val sampleSpace = CoupleSpace(
        id = "preview_space",
        partner1Name = "Alex",
        partner2Name = "Sam",
        anniversaryYear = 2025,
        anniversaryMonth = 6,
        anniversaryDay = 25
    )
    val metrics = RelationshipDateCalculator.calculate(2025, 6, 25)

    MyApplicationTheme(darkTheme = false) {
        DashboardScreen(
            state = DashboardUiState(
                space = sampleSpace,
                metrics = metrics
            ),
            onMenuExpandedChanged = {},
            onShowDisconnectDialog = {},
            onConfirmDisconnect = {}
        )
    }
}

@Preview(name = "Dashboard - Dark", showBackground = true)
@Composable
private fun DashboardScreenDarkPreview() {
    val sampleSpace = CoupleSpace(
        id = "preview_space",
        partner1Name = "Alex",
        partner2Name = "Sam",
        anniversaryYear = 2025,
        anniversaryMonth = 6,
        anniversaryDay = 25
    )
    val metrics = RelationshipDateCalculator.calculate(2025, 6, 25)

    MyApplicationTheme(darkTheme = true) {
        DashboardScreen(
            state = DashboardUiState(
                space = sampleSpace,
                metrics = metrics
            ),
            onMenuExpandedChanged = {},
            onShowDisconnectDialog = {},
            onConfirmDisconnect = {}
        )
    }
}
