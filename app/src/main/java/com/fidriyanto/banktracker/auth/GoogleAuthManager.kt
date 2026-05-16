package com.fidriyanto.banktracker.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.fidriyanto.banktracker.data.prefs.SecurePrefs
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SecurePrefs
) {
    companion object {
        val SCOPES = listOf(
            "https://www.googleapis.com/auth/gmail.readonly",
            "https://www.googleapis.com/auth/spreadsheets"
        )
    }

    private val _consentRequired = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val consentRequired = _consentRequired.asSharedFlow()

    fun getSignInIntent(): Intent {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                com.google.android.gms.common.api.Scope(SCOPES[0]),
                com.google.android.gms.common.api.Scope(SCOPES[1])
            )
            .build()
        return GoogleSignIn.getClient(context, options).signInIntent
    }

    fun isSignedIn(): Boolean = GoogleSignIn.getLastSignedInAccount(context) != null

    fun getSignedInEmail(): String? = GoogleSignIn.getLastSignedInAccount(context)?.email

    suspend fun getValidAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            Log.d("GoogleAuth", "account=${account?.email}")
            account ?: return@withContext null
            val credential = GoogleAccountCredential.usingOAuth2(context, SCOPES)
            credential.selectedAccount = account.account
            val token = credential.token
            Log.d("GoogleAuth", "token=${if (token != null) "ok" else "null"}")
            token
        } catch (e: UserRecoverableAuthException) {
            Log.w("GoogleAuth", "consent required, emitting intent")
            e.intent?.let { _consentRequired.tryEmit(it) }
            null
        } catch (e: Exception) {
            Log.e("GoogleAuth", "getValidAccessToken failed", e)
            null
        }
    }

    fun signOut() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, options).signOut()
        prefs.googleAccountEmail = ""
    }
}
