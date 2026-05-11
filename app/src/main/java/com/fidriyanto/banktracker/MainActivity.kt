package com.fidriyanto.banktracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.fidriyanto.banktracker.ui.navigation.AppNavigation
import com.fidriyanto.banktracker.ui.theme.BankTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BankTrackerTheme { AppNavigation() } }
    }
}
