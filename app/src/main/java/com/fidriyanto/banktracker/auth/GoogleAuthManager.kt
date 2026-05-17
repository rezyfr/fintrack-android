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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface GoogleAuthManager{
    val consentRequired :  SharedFlow<Intent>

    fun getSignInIntent(): Intent

    fun isSignedIn(): Boolean

    fun getSignedInEmail(): String?

    suspend fun getValidAccessToken(): String?

    fun signOut()
}
