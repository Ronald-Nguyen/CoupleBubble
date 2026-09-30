package com.aistudio.couplebubble.qxztrw

import android.content.Context
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private enum class AppScreen {
    LOADING,
    UNPAIRED,
    PAIRED
}

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
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            AppScreen.LOADING -> {
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
            AppScreen.UNPAIRED -> {
                lastUnpairedState?.let { unpairedState ->
                    PairingScreen(
                        state = unpairedState,
                        onTabSelected = viewModel::onTabSelected,
                        onEnteredCodeChanged = viewModel::onEnteredCodeChanged,
                        onCopyCodeClicked = viewModel::onCopyCodeSuccess,
                        onGenerateNewCode = viewModel::onGenerateNewCode,
                        onConnectClicked = viewModel::onConnectClicked,
                        onOpenDemoSpace = viewModel::onOpenDemoSpace,
                        onSaveSpaceSetup = viewModel::onSaveSpaceSetup,
                        onDismissSetupDialog = { viewModel.setShowSetupSpaceDialog(false) },
                        onSignInWithGoogle = viewModel::onSignInWithGoogle,
                        onSignInWithGoogleClick = { actCtx ->
                            viewModel.onSignInWithGoogleClicked(
                                activityContext = actCtx,
                                onError = { errorMsg ->
                                    Toast.makeText(actCtx, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        onSignOutGoogle = { viewModel.onSignOutGoogle(context) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            AppScreen.PAIRED -> {
                BackHandler {
                    viewModel.setShowDisconnectDialog(true)
                }

                lastPairedState?.let { pairedState ->
                    DashboardScreen(
                        state = pairedState,
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
                        onShowGoogleBackupDialog = viewModel::setShowGoogleBackupDialog,
                        onSaveSpaceSetup = viewModel::onSaveSpaceSetup,
                        onSignInWithGoogle = viewModel::onSignInWithGoogle,
                        onSignInWithGoogleClick = { actCtx ->
                            viewModel.onSignInWithGoogleClicked(
                                activityContext = actCtx,
                                onError = { errorMsg ->
                                    Toast.makeText(actCtx, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        onSignOutGoogle = { viewModel.onSignOutGoogle(context) },
                        onUploadProfilePhotoBytes = { isPartner1, bytes ->
                            viewModel.onUploadProfilePhoto(
                                isPartner1 = isPartner1,
                                imageBytes = bytes,
                                onSuccess = {
                                    Toast.makeText(context, context.getString(R.string.profile_photo_saved), Toast.LENGTH_SHORT).show()
                                },
                                onError = {
                                    Toast.makeText(context, context.getString(R.string.profile_photo_upload_failed), Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        onUpdatePartnerColor = { color, isPartner1 ->
                            viewModel.onUpdatePartnerColor(color, isPartner1)
                        },
                        onSwapPartnerRoles = {
                            viewModel.swapPartnerRoles()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
