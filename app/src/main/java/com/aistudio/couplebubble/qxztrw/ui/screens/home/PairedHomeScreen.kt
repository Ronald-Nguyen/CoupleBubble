package com.aistudio.couplebubble.qxztrw.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.calendar.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.ui.screens.dashboard.DashboardScreen
import com.aistudio.couplebubble.qxztrw.ui.screens.notes.NotesScreen
import com.aistudio.couplebubble.qxztrw.ui.screens.notes.NotesUiState
import com.aistudio.couplebubble.qxztrw.ui.screens.notes.previewAuthorColors
import com.aistudio.couplebubble.qxztrw.ui.screens.notes.previewLabels
import com.aistudio.couplebubble.qxztrw.ui.screens.notes.previewNotes
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme

private data class MainTabSpec(
    val tab: MainTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val icon: ImageVector
)

private val mainTabs = listOf(
    MainTabSpec(MainTab.US, R.string.nav_tab_us, Icons.Filled.Home, Icons.Outlined.Home),
    MainTabSpec(MainTab.NOTES, R.string.nav_tab_notes, Icons.AutoMirrored.Filled.StickyNote2, Icons.AutoMirrored.Outlined.StickyNote2)
)

/**
 * Paired shell with the bottom navigation. The tab content gets the bar's padding with its insets consumed,
 * so the screens' own Scaffolds don't pad for the system bars a second time.
 */
@Composable
fun PairedHomeScreen(
    selectedTab: MainTab,
    isNoteOpen: Boolean,
    isOffline: Boolean,
    hasUnreadNotes: Boolean = false,
    onTabSelected: (MainTab) -> Unit,
    onTabReselected: (MainTab) -> Unit = {},
    usContent: @Composable (Modifier) -> Unit,
    notesContent: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val saveableStateHolder = rememberSaveableStateHolder()

    // Back from the notes overview returns to "Uns" before the system closes the app
    BackHandler(enabled = selectedTab == MainTab.NOTES && !isNoteOpen) {
        onTabSelected(MainTab.US)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AnimatedVisibility(
                visible = isOffline,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(spring(stiffness = Spring.StiffnessMediumLow)),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                OfflineBanner()
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !isNoteOpen,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        modifier = Modifier.testTag("bottom_navigation")
                    ) {
                        mainTabs.forEach { spec ->
                            val isSelected = spec.tab == selectedTab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTabSelected(spec.tab)
                                    } else {
                                        onTabReselected(spec.tab)
                                    }
                                },
                                icon = {
                                    val iconVector = if (isSelected) spec.selectedIcon else spec.icon
                                    if (spec.tab == MainTab.NOTES && hasUnreadNotes) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.testTag("notes_tab_badge")
                                                )
                                            }
                                        ) {
                                            Icon(
                                                imageVector = iconVector,
                                                contentDescription = null
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = null
                                        )
                                    }
                                },
                                label = { Text(stringResource(spec.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                modifier = Modifier.testTag("bottom_nav_${spec.tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                    slideInHorizontally(spring(stiffness = Spring.StiffnessLow)) { direction * it / 8 })
                    .togetherWith(fadeOut(spring(stiffness = Spring.StiffnessMedium)))
            },
            label = "main_tab_transition"
        ) { tab ->
            saveableStateHolder.SaveableStateProvider(tab) {
                val contentModifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                when (tab) {
                    MainTab.US -> usContent(contentModifier)
                    MainTab.NOTES -> notesContent(contentModifier)
                }
            }
        }
    }
}

/** Calm tonal hint that changes are kept and synced later; no alarm colors. */
@Composable
private fun OfflineBanner() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("offline_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = stringResource(R.string.offline_banner), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Preview(name = "Paired Home Notes Light", showBackground = true)
@Composable
private fun PairedHomeNotesPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        PairedHomeScreen(
            selectedTab = MainTab.NOTES,
            isNoteOpen = false,
            isOffline = false,
            onTabSelected = {},
            usContent = { Box(it) },
            notesContent = {
                NotesScreen(
                    state = NotesUiState(
                        notes = previewNotes,
                        hasAnyNotes = true,
                        labels = previewLabels,
                        authorColors = previewAuthorColors
                    ),
                    onEvent = {},
                    modifier = it
                )
            }
        )
    }
}

@Preview(name = "Paired Home Us Dark", showBackground = true)
@Composable
private fun PairedHomeUsDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        PairedHomeScreen(
            selectedTab = MainTab.US,
            isNoteOpen = false,
            isOffline = true,
            onTabSelected = {},
            usContent = {
                DashboardScreen(
                    state = DashboardUiState(
                        space = CoupleSpace(partner1Name = "Alex", partner2Name = "Sam"),
                        metrics = RelationshipDateCalculator.calculate(2025, 6, 25)
                    ),
                    onEvent = {},
                    modifier = it
                )
            },
            notesContent = { Box(it) }
        )
    }
}
