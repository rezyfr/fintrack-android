package com.fidriyanto.banktracker.data.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePrefs @Inject constructor(@ApplicationContext context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    private val prefs = EncryptedSharedPreferences.create(
        context, "secure_prefs", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var googleAccessToken: String
        get() = prefs.getString("google_access_token", "") ?: ""
        set(v) = prefs.edit().putString("google_access_token", v).apply()

    var googleAccountEmail: String
        get() = prefs.getString("google_account_email", "") ?: ""
        set(v) = prefs.edit().putString("google_account_email", v).apply()

    var gmailSenderFilter: String
        get() = prefs.getString("gmail_sender_filter", "from:bangkokbank.com") ?: "from:bangkokbank.com"
        set(v) = prefs.edit().putString("gmail_sender_filter", v).apply()
}
