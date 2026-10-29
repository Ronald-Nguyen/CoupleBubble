package com.aistudio.couplebubble.qxztrw.data.demo

import android.content.Context
import com.aistudio.couplebubble.qxztrw.data.auth.GoogleSignInClient
import com.google.firebase.auth.FirebaseUser

/** Google sign-in needs Firebase; the demo mode explains that instead of failing with a setup error. */
class GoogleSignInUnavailableInDemoException : UnsupportedOperationException("Google sign-in is not available in the demo mode")

class DemoGoogleSignInClient : GoogleSignInClient {
    override suspend fun signIn(activityContext: Context): Result<FirebaseUser> =
        Result.failure(GoogleSignInUnavailableInDemoException())

    override suspend fun signOut() = Unit
}
