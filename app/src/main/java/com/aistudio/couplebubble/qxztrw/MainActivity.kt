package com.aistudio.couplebubble.qxztrw

import android.os.Bundle
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistudio.couplebubble.qxztrw.data.CoupleSessionPreferences
import com.aistudio.couplebubble.qxztrw.repository.FirebaseCoupleRepository
import com.aistudio.couplebubble.qxztrw.ui.CoupleMainState
import com.aistudio.couplebubble.qxztrw.ui.CoupleViewModel
import com.aistudio.couplebubble.qxztrw.ui.screens.DashboardScreen
import com.aistudio.couplebubble.qxztrw.ui.screens.PairingScreen
import com.aistudio.couplebubble.qxztrw.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CoupleBubbleApp()
            }
        }
    }
}

@Composable
fun CoupleBubbleApp() {
    val context = LocalContext.current
    val preferences = remember(context) { CoupleSessionPreferences(context.applicationContext) }
    val viewModel: CoupleViewModel = viewModel {
        CoupleViewModel(
            repository = FirebaseCoupleRepository(),
            preferences = preferences
        )
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Crossfade(
        targetState = uiState,
        animationSpec = tween(400),
        label = "ScreenTransition"
    ) { state ->
        when (state) {
            is CoupleMainState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            is CoupleMainState.Unpaired -> {
                PairingScreen(
                    state = state.state,
                    onTabSelected = viewModel::onTabSelected,
                    onEnteredCodeChanged = viewModel::onEnteredCodeChanged,
                    onCopyCodeClicked = viewModel::onCopyCodeSuccess,
                    onGenerateNewCode = viewModel::onGenerateNewCode,
                    onConnectClicked = viewModel::onConnectClicked,
                    onOpenDemoSpace = viewModel::onOpenDemoSpace,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is CoupleMainState.Paired -> {
                BackHandler {
                    viewModel.setShowDisconnectDialog(true)
                }

                DashboardScreen(
                    state = state.state,
                    onMenuExpandedChanged = viewModel::setMenuExpanded,
                    onShowDisconnectDialog = viewModel::setShowDisconnectDialog,
                    onConfirmDisconnect = viewModel::onConfirmDisconnect,
                    onShowEditNamesDialog = viewModel::setShowEditNamesDialog,
                    onUpdatePartnerNames = viewModel::onUpdatePartnerNames,
                    onShowAddMemoryDialog = viewModel::setShowAddMemoryDialog,
                    onAddMemory = viewModel::onAddMemory,
                    onShowEditMemoryDialog = viewModel::setMemoryToEdit,
                    onShowDeleteMemoryDialog = viewModel::setMemoryToDelete,
                    onUpdateMemory = viewModel::onUpdateMemory,
                    onDeleteMemory = viewModel::onDeleteMemory,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
