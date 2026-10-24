package com.fidriyanto.banktracker.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.fidriyanto.banktracker.R

// ac: loading-states-for-fetches — the one loading state every screen shows while its first fetch
// from the backend runs. Pass a size modifier to place it inside a scrolling column.
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier.fillMaxSize()) {
    val description = stringResource(R.string.loading)
    Box(modifier.semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
