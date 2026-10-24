package com.fidriyanto.banktracker.domain.model

// Where the app keeps the user's data. LOCAL keeps everything on the phone. SERVER uses a FinTrack server.
enum class StorageMode { LOCAL, SERVER }

// A FinTrack server: its address without a trailing slash, and the token the server checks.
data class ServerConfig(val baseUrl: String, val token: String)

// The storage the app uses. [chosen] is false until the user picks a mode in setup. Until then [mode] is
// SERVER when the build has a built-in server, and LOCAL otherwise.
data class StorageSettings(val mode: StorageMode, val server: ServerConfig?, val chosen: Boolean)

// The answer of a FinTrack server to the connection test in setup.
sealed interface ServerCheck {
    // The server answered and accepted the token. [accounts] is the number of accounts it already holds.
    data class Ok(val accounts: Int) : ServerCheck

    // The server answered, but it refused the token.
    data object BadToken : ServerCheck

    // Something answered at the address, but it is not a FinTrack server.
    data object NotFinTrack : ServerCheck

    // Nothing answered at the address.
    data object Unreachable : ServerCheck

    // The text is not a web address.
    data object BadAddress : ServerCheck

    // The address uses http://. Android lets the app reach a server over https:// only.
    data object NeedsHttps : ServerCheck
}
