package com.aistudio.couplebubble.qxztrw.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.aistudio.couplebubble.qxztrw.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

private fun Context.findActivity(): android.app.Activity? {
    var current: Context? = this
    while (current is android.content.ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}

class GoogleAuthClient(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    fun getWebClientId(): String {
        return try {
            val candidate = context.getString(R.string.default_web_client_id).trim()
            if (candidate.isNotBlank() && !candidate.startsWith("YOUR_GOOGLE_WEB_CLIENT_ID")) {
                candidate
            } else {
                getWebClientIdFallback()
            }
        } catch (e: Exception) {
            getWebClientIdFallback()
        }
    }

    private fun getWebClientIdFallback(): String {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                val candidate = context.getString(resId).trim()
                if (candidate.isNotBlank() && !candidate.startsWith("YOUR_GOOGLE_WEB_CLIENT_ID")) {
                    return candidate
                }
            }
            ""
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun signIn(activityContext: Context = context): Result<FirebaseUser> {
        val webClientId = getWebClientId()
        if (webClientId.isBlank()) {
            val ex = IllegalStateException("Web-Client-ID ist noch nicht konfiguriert. Bitte stelle sicher, dass google-services.json im app/-Verzeichnis vorhanden ist.")
            Log.e("CoupleBubbleAuth", "Login failed: Configuration error: Web client ID is missing or invalid", ex)
            return Result.failure(ex)
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val targetActivity = activityContext.findActivity() ?: context.findActivity()
        val targetContext = targetActivity ?: activityContext

        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = targetContext
            )
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    Result.success(user)
                } else {
                    val ex = IllegalStateException("FirebaseUser ist null nach erfolgreicher Authentifizierung.")
                    Log.e("CoupleBubbleAuth", "Login failed", ex)
                    Result.failure(ex)
                }
            } else {
                val ex = IllegalArgumentException("Unerwarteter Credential-Typ erhalten: ${credential::class.java.name}")
                Log.e("CoupleBubbleAuth", "Login failed", ex)
                Result.failure(ex)
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d("CoupleBubbleAuth", "Sign-in cancelled by user")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("CoupleBubbleAuth", "Login failed", e)
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            // Ignore error on credential clearing
        }
        try {
            firebaseAuth.signOut()
        } catch (e: Exception) {
            // Ignore error on auth sign out
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return try {
            firebaseAuth.currentUser
        } catch (e: Exception) {
            null
        }
    }
}
