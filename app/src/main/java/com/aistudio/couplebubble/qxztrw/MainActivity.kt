package com.aistudio.couplebubble.qxztrw

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.aistudio.couplebubble.qxztrw.ui.components.LocalBitmapLoader
import com.aistudio.couplebubble.qxztrw.ui.screens.dashboard.DashboardScreen
import com.aistudio.couplebubble.qxztrw.ui.screens.dashboard.DashboardViewModel
import com.aistudio.couplebubble.qxztrw.ui.screens.home.CoupleEffect
import com.aistudio.couplebubble.qxztrw.ui.screens.home.AppRoute
import com.aistudio.couplebubble.qxztrw.ui.screens.home.AppViewModel
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardEvent
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PairedHomeScreen
import com.aistudio.couplebubble.qxztrw.ui.screens.notes.NotesScreen
import com.aistudio.couplebubble.qxztrw.ui.screens.notes.NotesEvent
import com.aistudio.couplebubble.qxztrw.ui.screens.notes.NotesViewModel
import com.aistudio.couplebubble.qxztrw.ui.screens.pairing.PairingScreen
import com.aistudio.couplebubble.qxztrw.ui.screens.pairing.PairingViewModel
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CoupleBubbleApplication).container
        setContent {
            CoupleBubbleTheme {
                CompositionLocalProvider(
                    LocalBitmapLoader provides { container.localImageStore.loadSampledBitmap(it) }
                ) {
                    CoupleBubbleApp(container)
                }
            }
        }
    }
}

@Composable
fun CoupleBubbleApp(container: AppContainer) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appViewModel: AppViewModel = viewModel { container.createAppViewModel() }
    val pairingViewModel: PairingViewModel = viewModel { container.createPairingViewModel() }
    val dashboardViewModel: DashboardViewModel = viewModel { container.createDashboardViewModel() }
    val notesViewModel: NotesViewModel = viewModel { container.createNotesViewModel(context.resources) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(appViewModel) {
        appViewModel.tryRestoreWithGoogle(context)
    }

    LaunchedEffect(pairingViewModel, dashboardViewModel, notesViewModel) {
        merge(pairingViewModel.effects, dashboardViewModel.effects, notesViewModel.effects).collect { effect ->
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

    val route by appViewModel.route.collectAsStateWithLifecycle()
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
            AppRoute.UNPAIRED -> {
                PairingScreen(
                    state = pairingState,
                    onEvent = pairingViewModel::onEvent,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            AppRoute.PAIRED -> {
                BackHandler {
                    dashboardViewModel.onEvent(DashboardEvent.ShowDisconnectDialog(true))
                }

                lastDashboardState?.let { pairedState ->
                    val notesState by notesViewModel.uiState.collectAsStateWithLifecycle()
                    PairedHomeScreen(
                        selectedTab = pairedState.dialogs.selectedTab,
                        isNoteOpen = notesState.openNote != null,
                        onTabSelected = { dashboardViewModel.onEvent(DashboardEvent.SelectMainTab(it)) },
                        usContent = { contentModifier ->
                            DashboardScreen(
                                state = pairedState,
                                onEvent = dashboardViewModel::onEvent,
                                snackbarHostState = snackbarHostState,
                                modifier = contentModifier,
                            )
                        },
                        notesContent = { contentModifier ->
                            NotesScreen(
                                state = notesState,
                                onEvent = notesViewModel::onEvent,
                                modifier = contentModifier,
                                snackbarHostState = snackbarHostState,
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
