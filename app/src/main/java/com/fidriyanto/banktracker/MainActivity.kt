package com.fidriyanto.banktracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.fidriyanto.banktracker.ui.navigation.AppNavigation
import com.fidriyanto.banktracker.ui.theme.BankTrackerTheme
import com.fidriyanto.banktracker.ui.theme.LocalIsDarkTheme
import com.fidriyanto.banktracker.ui.theme.LocalThemeToggle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getPreferences(MODE_PRIVATE)
        setContent {
            // ac: light-pine-theme — fresh install defaults to light; the toggle still persists a choice
            var darkTheme by remember { mutableStateOf(prefs.getBoolean("dark_theme", false)) }
            CompositionLocalProvider(
                LocalIsDarkTheme provides darkTheme,
                LocalThemeToggle provides {
                    darkTheme = !darkTheme
                    prefs.edit().putBoolean("dark_theme", darkTheme).apply()
                },
            ) {
                BankTrackerTheme(darkTheme = darkTheme) {
                    AppNavigation()
                }
            }
        }
    }
}
