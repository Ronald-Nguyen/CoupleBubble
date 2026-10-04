package com.aistudio.couplebubble.qxztrw.auth

import android.content.Context
import com.google.firebase.auth.FirebaseUser

/** Google sign-in via Credential Manager; [signIn] needs an Activity context to show the account picker. */
interface GoogleSignInClient {
    suspend fun signIn(activityContext: Context): Result<FirebaseUser>
    suspend fun signOut()
}
