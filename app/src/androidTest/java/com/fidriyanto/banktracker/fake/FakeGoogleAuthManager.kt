package com.fidriyanto.banktracker.fake

import android.content.Intent
import com.fidriyanto.banktracker.auth.GoogleAuthManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeGoogleAuthManager @Inject constructor() : GoogleAuthManager {
    var signedIn = false
    var email: String? = null
    var accessToken: String? = "fake-token"
    var signOutCalled = false

    private val _consentRequired = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    override val consentRequired: SharedFlow<Intent> = _consentRequired.asSharedFlow()

    override fun getSignInIntent(): Intent = Intent()
    override fun isSignedIn(): Boolean = signedIn
    override fun getSignedInEmail(): String? = email
    override suspend fun getValidAccessToken(): String? = accessToken
    override fun signOut() { signedIn = false; email = null; signOutCalled = true }

    fun reset() {
        signedIn = false
        email = null
        accessToken = "fake-token"
        signOutCalled = false
    }
}
