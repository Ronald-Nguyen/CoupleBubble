package com.aistudio.couplebubble.qxztrw.ui.screens.home

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.aistudio.couplebubble.qxztrw.BuildConfig
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.rethrowIfCancellation
import com.aistudio.couplebubble.qxztrw.core.ui.UiText
import com.aistudio.couplebubble.qxztrw.data.auth.GoogleSignInClient
import com.aistudio.couplebubble.qxztrw.data.demo.GoogleSignInUnavailableInDemoException
import com.aistudio.couplebubble.qxztrw.data.local.SessionStore
import com.aistudio.couplebubble.qxztrw.domain.repository.SpaceRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Google sign-in, shared by the pairing screen and the dashboard's backup dialog so both show the same
 * progress. A successful sign-in returns to the space anchored at `/users/{uid}`.
 */
class GoogleAuthController(
    private val repository: SpaceRepository,
    private val client: GoogleSignInClient,
    private val sessionStore: SessionStore
) {
    private val _state = MutableStateFlow(GoogleAuthState())
    val state: StateFlow<GoogleAuthState> = _state.asStateFlow()

    /** Shows the account picker; returns the message to show, or `null` on success or when the user cancelled. */
    suspend fun signIn(activityContext: Context): UiText? {
        _state.value = GoogleAuthState(isLoading = true)
        // Reset the spinner even when the caller is cancelled mid-sign-in, or it would stay on for the process
        try {
            val failure = try {
                signInWith { client.signIn(activityContext) }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                e
            }
            if (failure == null || failure.isUserCancellation()) {
                return null
            }
            val errorText = errorText(failure)
            _state.value = GoogleAuthState(isLoading = false, error = errorText)
            return errorText
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    /** Picker-free sign-in with an account that already used the app; failures stay silent. */
    suspend fun signInSilently(activityContext: Context) {
        _state.value = GoogleAuthState(isLoading = true)
        try {
            signInWith { client.signInSilently(activityContext) }
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            // No authorized account: the user pairs or signs in manually
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /** Forgets the Google account; the repository keeps the Firebase user while a space is active. */
    suspend fun signOut() {
        try {
            client.signOut()
        } catch (e: Exception) {
            e.rethrowIfCancellation()
        }
        repository.signOutUser()
    }

    /** Runs [signIn] and restores the space anchored at `/users/{uid}`; returns the failure or `null`. */
    private suspend fun signInWith(signIn: suspend () -> Result<FirebaseUser>): Throwable? {
        val previousUid = repository.currentAuthUid
        val result = signIn()
        result.getOrNull()?.let { firebaseUser ->
            val profile = repository.signInWithGoogleUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
                previousUid = previousUid
            ).getOrNull()
            profile?.coupleId?.takeIf { it.isNotBlank() }?.let { sessionStore.saveActiveCoupleId(it) }
        }
        return result.exceptionOrNull()
    }

    private fun Throwable.isUserCancellation(): Boolean =
        this is GetCredentialCancellationException || cause is GetCredentialCancellationException

    private fun errorText(throwable: Throwable): UiText =
        googleSignInErrorText(throwable, showDeveloperDetails = BuildConfig.DEBUG)
}

/**
 * Maps a sign-in failure to a message. Setup problems (missing SHA-1, disabled provider) and raw exception
 * texts are only spelled out in debug builds; release users get [R.string.google_sign_in_unavailable].
 */
internal fun googleSignInErrorText(throwable: Throwable, showDeveloperDetails: Boolean): UiText {
    val msg = throwable.message ?: throwable.localizedMessage ?: ""
    val isSha1Missing = throwable is GetCredentialException &&
        (msg.contains("[10]") || msg.contains("[16]") || msg.contains("[28444]") || msg.contains("DEVELOPER_ERROR"))
    val isProviderDisabled = throwable is FirebaseAuthException &&
        (throwable.errorCode == "ERROR_OPERATION_NOT_ALLOWED" || msg.contains("CONFIGURATION_NOT_FOUND"))
    return when {
        throwable is GoogleSignInUnavailableInDemoException -> UiText.Resource(R.string.google_sign_in_demo_unavailable)
        throwable is NoCredentialException -> UiText.Resource(R.string.google_error_no_credentials)
        throwable is FirebaseNetworkException -> UiText.Resource(R.string.google_error_network)
        !showDeveloperDetails -> UiText.Resource(R.string.google_sign_in_unavailable)
        isSha1Missing -> UiText.Resource(R.string.google_error_sha1_missing)
        isProviderDisabled -> UiText.Resource(R.string.google_error_provider_disabled)
        msg.isNotBlank() -> UiText.Resource(R.string.google_sign_in_error_prefix, listOf(msg))
        else -> UiText.Resource(R.string.google_sign_in_failed)
    }
}
