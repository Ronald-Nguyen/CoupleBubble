package com.aistudio.couplebubble.qxztrw.ui.screens.home

import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleSignInErrorTextTest {

    private val sha1Failure = GetCredentialUnknownException("[10] DEVELOPER_ERROR")

    @Test
    fun `debug builds name the missing SHA-1`() {
        assertEquals(
            UiText.Resource(R.string.google_error_sha1_missing),
            googleSignInErrorText(sha1Failure, showDeveloperDetails = true)
        )
    }

    @Test
    fun `release builds hide setup details`() {
        assertEquals(
            UiText.Resource(R.string.google_sign_in_unavailable),
            googleSignInErrorText(sha1Failure, showDeveloperDetails = false)
        )
    }

    @Test
    fun `release builds never show raw exception text`() {
        assertEquals(
            UiText.Resource(R.string.google_sign_in_unavailable),
            googleSignInErrorText(IllegalStateException("internal detail"), showDeveloperDetails = false)
        )
    }

    @Test
    fun `missing account is explained in every build`() {
        assertEquals(
            UiText.Resource(R.string.google_error_no_credentials),
            googleSignInErrorText(NoCredentialException(), showDeveloperDetails = false)
        )
    }
}
