package com.fidriyanto.banktracker.domain.model

import java.net.URI

// Turns what the user typed into a server address: https:// when no scheme is given, and no trailing slash.
// Returns null when the text is not an http or https address with a host.
fun normalizeServerAddress(text: String): String? {
    // Look for the scheme before the slashes go, so "https://" alone does not become a host named "https".
    val hasScheme = "://" in text
    val trimmed = text.trim().trimEnd('/')
    if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
    val withScheme = if (hasScheme) trimmed else "https://$trimmed"
    val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
    if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrEmpty()) return null
    return withScheme
}
