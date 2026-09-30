package com.aistudio.couplebubble.qxztrw.auth

import android.content.Context
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

class GoogleAuthClient(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    fun getWebClientId(): String {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            context.getString(R.string.default_web_client_id)
        }
    }

    suspend fun signIn(activityContext: Context = context): Result<FirebaseUser> {
        val webClientId = getWebClientId()
        if (webClientId.isBlank() || webClientId.startsWith("YOUR_GOOGLE_WEB_CLIENT_ID")) {
            return Result.failure(
                IllegalStateException("Web-Client-ID ist noch nicht konfiguriert. Bitte trage deine Google Web-Client-ID in strings.xml oder google-services.json ein.")
            )
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
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
                    Result.failure(IllegalStateException("FirebaseUser ist null nach erfolgreicher Authentifizierung."))
                }
            } else {
                Result.failure(IllegalArgumentException("Unerwarteter Credential-Typ erhalten: ${credential::class.java.name}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: Exception) {
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
