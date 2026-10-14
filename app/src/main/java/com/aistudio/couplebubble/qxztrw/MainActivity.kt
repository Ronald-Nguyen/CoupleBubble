package com.aistudio.couplebubble.qxztrw

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistudio.couplebubble.qxztrw.core.di.AppContainer
import com.aistudio.couplebubble.qxztrw.core.ui.asString
import com.aistudio.couplebubble.qxztrw.domain.model.ThemeMode
import com.aistudio.couplebubble.qxztrw.ui.components.LocalBitmapLoader
import com.aistudio.couplebubble.qxztrw.ui.screens.dashboard.DashboardScreen
import com.aistudio.couplebubble.qxztrw.ui.screens.dashboard.DashboardViewModel
import com.aistudio.couplebubble.qxztrw.ui.screens.home.CoupleEffect
import com.aistudio.couplebubble.qxztrw.ui.screens.home.AppRoute
import com.aistudio.couplebubble.qxztrw.ui.screens.home.AppViewModel
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardEvent
import com.aistudio.couplebubble.qxztrw.ui.screens.home.MainTab
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PairedHomeScreen
import com.aistudio.couplebubble.qxztrw.ui.screens.notes.NotesScreen
import com.aistudio.couplebubble.qxztrw.ui.screens.notes.NotesEvent
import com.aistudio.couplebubble.qxztrw.ui.screens.notes.NotesViewModel
import com.aistudio.couplebubble.qxztrw.ui.screens.onboarding.OnboardingScreen
import android.content.Intent
import com.aistudio.couplebubble.qxztrw.domain.model.DeepLinkTarget
import com.aistudio.couplebubble.qxztrw.domain.model.parseDeepLink
import com.aistudio.couplebubble.qxztrw.ui.screens.pairing.PairingScreen
import com.aistudio.couplebubble.qxztrw.ui.screens.pairing.PairingViewModel
import com.aistudio.couplebubble.qxztrw.ui.screens.settings.SettingsEvent
import com.aistudio.couplebubble.qxztrw.ui.screens.settings.SettingsScreen
import com.aistudio.couplebubble.qxztrw.ui.screens.settings.SettingsViewModel
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val pendingDeepLink = MutableStateFlow<DeepLinkTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseDeepLink(intent)?.let { pendingDeepLink.value = it }
        enableEdgeToEdge()
        val container = (application as CoupleBubbleApplication).container
        setContent {
            val themeMode by container.sessionStore.themeModeFlow.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val isSystemInDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DisposableEffect(darkTheme) {
                val statusBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(
                    statusBarStyle = statusBarStyle,
                    navigationBarStyle = statusBarStyle
                )
                onDispose {}
            }

            CoupleBubbleTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalBitmapLoader provides { container.localImageStore.loadSampledBitmap(it) }
                ) {
                    CoupleBubbleApp(container, pendingDeepLink)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseDeepLink(intent)?.let { pendingDeepLink.value = it }
    }
}

@Composable
fun CoupleBubbleApp(
    container: AppContainer,
    pendingDeepLink: MutableStateFlow<DeepLinkTarget?> = remember { MutableStateFlow(null) }
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appViewModel: AppViewModel = viewModel { container.createAppViewModel() }
    val pairingViewModel: PairingViewModel = viewModel { container.createPairingViewModel() }
    val dashboardViewModel: DashboardViewModel = viewModel { container.createDashboardViewModel() }
    val notesViewModel: NotesViewModel = viewModel { container.createNotesViewModel(context.resources) }
    val settingsViewModel: SettingsViewModel = viewModel { container.createSettingsViewModel() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(appViewModel) {
        appViewModel.tryRestoreWithGoogle(context)
    }

    val route by appViewModel.route.collectAsStateWithLifecycle()
    val isRestored by container.sessionRestorer.isRestored.collectAsStateWithLifecycle()
    val deepLinkTarget by pendingDeepLink.collectAsStateWithLifecycle()

    LaunchedEffect(isRestored, route, deepLinkTarget) {
        if (!isRestored) return@LaunchedEffect
        val target = deepLinkTarget ?: return@LaunchedEffect
        pendingDeepLink.value = null

        if (route != AppRoute.PAIRED) return@LaunchedEffect

        settingsViewModel.onEvent(SettingsEvent.Close)

        when (target) {
            is DeepLinkTarget.Moment -> {
                dashboardViewModel.onEvent(DashboardEvent.SelectMainTab(MainTab.US))
                dashboardViewModel.onEvent(DashboardEvent.HighlightMemory(target.memoryId))
            }
            DeepLinkTarget.Milestones -> {
                dashboardViewModel.onEvent(DashboardEvent.SelectMainTab(MainTab.US))
                dashboardViewModel.onEvent(DashboardEvent.ShowMilestoneSettingsDialog(true))
            }
            DeepLinkTarget.Reunion -> {
                dashboardViewModel.onEvent(DashboardEvent.SelectMainTab(MainTab.US))
                dashboardViewModel.onEvent(DashboardEvent.ShowReunionDialog(true))
            }
            DeepLinkTarget.Home -> {
                dashboardViewModel.onEvent(DashboardEvent.SelectMainTab(MainTab.US))
            }
            DeepLinkTarget.NewMoment -> {
                dashboardViewModel.onEvent(DashboardEvent.SelectMainTab(MainTab.US))
                dashboardViewModel.onEvent(DashboardEvent.CloseSearch)
                dashboardViewModel.onEvent(DashboardEvent.ShowAddMemoryDialog(true))
            }
            DeepLinkTarget.NewNote -> {
                dashboardViewModel.onEvent(DashboardEvent.SelectMainTab(MainTab.NOTES))
                notesViewModel.onEvent(NotesEvent.TabOpened)
                notesViewModel.onEvent(NotesEvent.CloseSearch)
                notesViewModel.onEvent(NotesEvent.CloseNote)
                notesViewModel.onEvent(NotesEvent.CreateNote)
            }
            DeepLinkTarget.NewList -> {
                dashboardViewModel.onEvent(DashboardEvent.SelectMainTab(MainTab.NOTES))
                notesViewModel.onEvent(NotesEvent.TabOpened)
                notesViewModel.onEvent(NotesEvent.CloseSearch)
                notesViewModel.onEvent(NotesEvent.CloseNote)
                notesViewModel.onEvent(NotesEvent.CreateChecklist)
            }
        }
    }

    LaunchedEffect(pairingViewModel, dashboardViewModel, notesViewModel, settingsViewModel) {
        merge(pairingViewModel.effects, dashboardViewModel.effects, notesViewModel.effects, settingsViewModel.effects).collect { effect ->
            when (effect) {
                is CoupleEffect.Toast -> Toast.makeText(
                    context,
                    effect.text.asString(context.resources),
                    if (effect.isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                ).show()
                // Snackbars suspend until dismissed; launch them so later effects are not held back
                is CoupleEffect.Snackbar -> launch {
                    val action = effect.action
                    val result = snackbarHostState.showSnackbar(
                        message = effect.text.asString(context.resources),
                        actionLabel = action?.label?.asString(context.resources),
                        duration = if (action != null) SnackbarDuration.Long else SnackbarDuration.Short
                    )
                    // The event goes back to whichever ViewModel sent the snackbar
                    when (val event = if (result == SnackbarResult.ActionPerformed) action?.onActionPerformed else action?.onDismissed) {
                        is DashboardEvent -> dashboardViewModel.onEvent(event)
                        is NotesEvent -> notesViewModel.onEvent(event)
                        is SettingsEvent -> settingsViewModel.onEvent(event)
                        else -> Unit
                    }
                }
                CoupleEffect.PartnersSwapped -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    launch { snackbarHostState.showSnackbar(context.getString(R.string.swap_partners_success)) }
                }
                is CoupleEffect.MemorySaved -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    launch { snackbarHostState.showSnackbar(effect.text.asString(context.resources)) }
                }
                is CoupleEffect.SavedToGallery -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val messageRes = if (effect.photoCount > 1) R.string.photos_saved_to_gallery else R.string.photo_saved_to_gallery
                    Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val pairingState by pairingViewModel.uiState.collectAsStateWithLifecycle()
    val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()

    // Ask for the notification permission once paired, so the partner's new moments can be announced
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val isPaired = route == AppRoute.PAIRED
    LaunchedEffect(isPaired) {
        if (isPaired && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Keeps the last dashboard on screen while the crossfade to the pairing screen runs after a disconnect
    var lastDashboardState by remember { mutableStateOf(dashboardState) }
    dashboardState?.let { lastDashboardState = it }

    Crossfade(
        targetState = route,
        animationSpec = tween(400),
        label = "ScreenTransition",
    ) { screen ->
        when (screen) {
            AppRoute.LOADING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            AppRoute.ONBOARDING -> {
                OnboardingScreen(
                    onComplete = { appViewModel.completeOnboarding() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            AppRoute.UNPAIRED -> {
                PairingScreen(
                    state = pairingState,
                    onEvent = pairingViewModel::onEvent,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            AppRoute.PAIRED -> {

                lastDashboardState?.let { pairedState ->
                    val notesState by notesViewModel.uiState.collectAsStateWithLifecycle()
                    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
                    val isOnline by dashboardViewModel.isOnline.collectAsStateWithLifecycle()
                    val dashboardListState = rememberLazyListState()
                    val notesGridState = rememberLazyStaggeredGridState()
                    val scope = rememberCoroutineScope()
                    LaunchedEffect(pairedState.dialogs.selectedTab) {
                        if (pairedState.dialogs.selectedTab == MainTab.NOTES) {
                            notesViewModel.onEvent(NotesEvent.TabOpened)
                        }
                    }
                    PairedHomeScreen(
                        selectedTab = pairedState.dialogs.selectedTab,
                        isNoteOpen = notesState.openNote != null,
                        isSettingsOpen = settingsState.isOpen,
                        isOffline = !isOnline,
                        hasUnreadNotes = notesState.hasUnreadNotes,
                        onTabSelected = { tab ->
                            dashboardViewModel.onEvent(DashboardEvent.SelectMainTab(tab))
                            if (tab == MainTab.NOTES) {
                                notesViewModel.onEvent(NotesEvent.TabOpened)
                            }
                        },
                        onTabReselected = { tab ->
                            scope.launch {
                                when (tab) {
                                    MainTab.US -> dashboardListState.animateScrollToItem(0)
                                    MainTab.NOTES -> notesGridState.animateScrollToItem(0)
                                }
                            }
                        },
                        usContent = { contentModifier ->
                            DashboardScreen(
                                state = pairedState,
                                onEvent = dashboardViewModel::onEvent,
                                onSettingsClick = { settingsViewModel.onEvent(SettingsEvent.Open) },
                                snackbarHostState = snackbarHostState,
                                listState = dashboardListState,
                                modifier = contentModifier,
                            )
                        },
                        notesContent = { contentModifier ->
                            NotesScreen(
                                state = notesState,
                                onEvent = notesViewModel::onEvent,
                                modifier = contentModifier,
                                snackbarHostState = snackbarHostState,
                                gridState = notesGridState,
                            )
                        },
                        settingsContent = { contentModifier ->
                            SettingsScreen(
                                state = settingsState,
                                onEvent = settingsViewModel::onEvent,
                                modifier = contentModifier,
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
