package com.aistudio.couplebubble.qxztrw

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.aistudio.couplebubble.qxztrw.ui.CoupleEffect
import com.aistudio.couplebubble.qxztrw.ui.CoupleMainState
import com.aistudio.couplebubble.qxztrw.ui.CoupleViewModel
import com.aistudio.couplebubble.qxztrw.ui.DashboardEvent
import com.aistudio.couplebubble.qxztrw.ui.NotesViewModel
import com.aistudio.couplebubble.qxztrw.ui.screens.NotesScreen
import com.aistudio.couplebubble.qxztrw.ui.screens.PairedHomeScreen
import com.aistudio.couplebubble.qxztrw.ui.screens.dashboard.DashboardScreen
import com.aistudio.couplebubble.qxztrw.ui.screens.pairing.PairingScreen
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme

private enum class AppScreen {
    LOADING,
    UNPAIRED,
    PAIRED
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CoupleBubbleApplication).container
        setContent {
            CoupleBubbleTheme {
                CoupleBubbleApp(container)
            }
        }
    }
}

@Composable
fun CoupleBubbleApp(container: AppContainer) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val viewModel: CoupleViewModel = viewModel { container.createCoupleViewModel() }
    val notesViewModel: NotesViewModel = viewModel { container.createNotesViewModel(viewModel, context.resources) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CoupleEffect.Toast -> Toast.makeText(
                    context,
                    effect.text.asString(context.resources),
                    if (effect.isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                ).show()
                // Snackbars suspend until dismissed; launch them so later effects are not held back
                is CoupleEffect.Snackbar -> launch { snackbarHostState.showSnackbar(effect.text.asString(context.resources)) }
                CoupleEffect.PartnersSwapped -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    launch { snackbarHostState.showSnackbar(context.getString(R.string.swap_partners_success)) }
                }
                is CoupleEffect.SavedToGallery -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val messageRes = if (effect.photoCount > 1) R.string.photos_saved_to_gallery else R.string.photo_saved_to_gallery
                    Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentScreen = when (uiState) {
        is CoupleMainState.Loading -> AppScreen.LOADING
        is CoupleMainState.Unpaired -> AppScreen.UNPAIRED
        is CoupleMainState.Paired -> AppScreen.PAIRED
    }

    var lastUnpairedState by remember { mutableStateOf((uiState as? CoupleMainState.Unpaired)?.state) }
    (uiState as? CoupleMainState.Unpaired)?.state?.let { lastUnpairedState = it }

    var lastPairedState by remember { mutableStateOf((uiState as? CoupleMainState.Paired)?.state) }
    (uiState as? CoupleMainState.Paired)?.state?.let { lastPairedState = it }

    Crossfade(
        targetState = currentScreen,
        animationSpec = tween(400),
        label = "ScreenTransition",
    ) { screen ->
        when (screen) {
            AppScreen.LOADING -> {
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
            AppScreen.UNPAIRED -> {
                lastUnpairedState?.let { unpairedState ->
                    PairingScreen(
                        state = unpairedState,
                        onEvent = viewModel::onPairingEvent,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            AppScreen.PAIRED -> {
                BackHandler {
                    viewModel.onDashboardEvent(DashboardEvent.ShowDisconnectDialog(true))
                }

                lastPairedState?.let { pairedState ->
                    val notesState by notesViewModel.uiState.collectAsStateWithLifecycle()
                    PairedHomeScreen(
                        selectedTab = pairedState.dialogs.selectedTab,
                        isNoteOpen = notesState.openNote != null,
                        onTabSelected = viewModel::onMainTabSelected,
                        usContent = { contentModifier ->
                            DashboardScreen(
                                state = pairedState,
                                onEvent = viewModel::onDashboardEvent,
                                snackbarHostState = snackbarHostState,
                                modifier = contentModifier,
                            )
                        },
                        notesContent = { contentModifier ->
                            NotesScreen(
                                state = notesState,
                                onEvent = notesViewModel::onEvent,
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
