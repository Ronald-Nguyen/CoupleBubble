package com.aistudio.couplebubble.qxztrw.ui.screens

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.ui.DashboardUiState
import com.aistudio.couplebubble.qxztrw.ui.MainTab
import com.aistudio.couplebubble.qxztrw.ui.NotesUiState
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
    onTabSelected: (MainTab) -> Unit,
    usContent: @Composable (Modifier) -> Unit,
    notesContent: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Back from the notes overview returns to "Uns" before the app-level handler asks to disconnect
    BackHandler(enabled = selectedTab == MainTab.NOTES && !isNoteOpen) {
        onTabSelected(MainTab.US)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
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
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) spec.selectedIcon else spec.icon,
                                        contentDescription = null
                                    )
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

@Preview(name = "Paired Home Notes Light", showBackground = true)
@Composable
private fun PairedHomeNotesPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        PairedHomeScreen(
            selectedTab = MainTab.NOTES,
            isNoteOpen = false,
            onTabSelected = {},
            usContent = { Box(it) },
            notesContent = {
                NotesScreen(
                    state = NotesUiState(notes = previewNotes, hasAnyNotes = true, authorColors = previewAuthorColors),
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
            onTabSelected = {},
            usContent = {
                DashboardScreen(
                    state = DashboardUiState(
                        space = CoupleSpace(partnerAName = "Alex", partnerBName = "Sam"),
                        metrics = RelationshipDateCalculator.calculate(2025, 6, 25)
                    ),
                    onMenuExpandedChanged = {},
                    onShowDisconnectDialog = {},
                    onConfirmDisconnect = {},
                    modifier = it
                )
            },
            notesContent = { Box(it) }
        )
    }
}
