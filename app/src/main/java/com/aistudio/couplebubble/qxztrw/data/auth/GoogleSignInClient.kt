package com.aistudio.couplebubble.qxztrw.data.auth

import android.content.Context
import com.google.firebase.auth.FirebaseUser

/** Google sign-in via Credential Manager; [signIn] needs an Activity context to show the account picker. */
interface GoogleSignInClient {
    suspend fun signIn(activityContext: Context): Result<FirebaseUser>

    /** Signs in without a picker, only with a Google account that already used this app (e.g. after a reinstall). */
    suspend fun signInSilently(activityContext: Context): Result<FirebaseUser> =
        Result.failure(UnsupportedOperationException())

    suspend fun signOut()
}
