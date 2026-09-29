package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CoupleMainState
import com.example.ui.CoupleViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PairingScreen
import com.example.ui.theme.MyApplicationTheme

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
fun CoupleBubbleApp(
    viewModel: CoupleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Crossfade(
        targetState = uiState,
        animationSpec = tween(400),
        label = "ScreenTransition"
    ) { state ->
        when (state) {
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
                // Handle system back gesture on dashboard to prompt disconnect dialog rather than closing abruptly
                BackHandler {
                    viewModel.setShowDisconnectDialog(true)
                }

                DashboardScreen(
                    state = state.state,
                    onMenuExpandedChanged = viewModel::setMenuExpanded,
                    onShowDisconnectDialog = viewModel::setShowDisconnectDialog,
                    onConfirmDisconnect = viewModel::onConfirmDisconnect,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
