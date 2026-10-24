package com.fidriyanto.banktracker.data.model

import com.fidriyanto.banktracker.domain.model.ServerConfig

// The server address and token compiled into this build from local.properties. A build without them
// leaves both empty, and the user then picks the storage in setup.
data class BuiltInServer(val baseUrl: String, val token: String) {
    val config: ServerConfig? get() = ServerConfig(baseUrl.trimEnd('/'), token).takeIf { baseUrl.isNotBlank() }
}
