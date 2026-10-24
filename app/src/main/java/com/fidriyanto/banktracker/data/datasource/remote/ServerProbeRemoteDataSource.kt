package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.domain.model.ServerCheck
import com.fidriyanto.banktracker.domain.model.ServerConfig

interface ServerProbeRemoteDataSource {
    suspend fun check(server: ServerConfig): ServerCheck
}
