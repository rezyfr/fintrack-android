package com.fidriyanto.banktracker.data.datasource.remote

import android.util.Log
import com.fidriyanto.banktracker.domain.model.ServerCheck
import com.fidriyanto.banktracker.domain.model.ServerConfig
import java.net.UnknownServiceException
import javax.inject.Inject

// ac: setup-storage-choice - tests a server before setup stores it: /health must answer ok, and
// /v1/accounts must accept the token. The number of accounts tells setup whether the server has data.
class ServerProbeRemoteDataSourceImpl @Inject constructor(
    private val service: ServerProbeApiService,
) : ServerProbeRemoteDataSource {
    override suspend fun check(server: ServerConfig): ServerCheck {
        // OkHttp throws UnknownServiceException when the network security policy blocks a plain http:// call.
        val health = runCatching { service.health("${server.baseUrl}/health") }.getOrElse {
            return when (it) {
                is IllegalArgumentException -> ServerCheck.BadAddress
                is UnknownServiceException -> ServerCheck.NeedsHttps
                else -> ServerCheck.Unreachable
            }
        }
        val body = runCatching { health.body()?.string().orEmpty() }.getOrDefault("")
        if (!health.isSuccessful || "\"ok\"" !in body) return ServerCheck.NotFinTrack
        val accounts = runCatching { service.accounts("${server.baseUrl}/v1/accounts", "Bearer ${server.token}") }
            .getOrElse { return ServerCheck.Unreachable }
        Log.d(TAG, "server check: health=${health.code()} accounts=${accounts.code()}")
        return when {
            accounts.code() == 401 -> ServerCheck.BadToken
            accounts.isSuccessful -> ServerCheck.Ok(accounts.body()?.size ?: 0)
            else -> ServerCheck.NotFinTrack
        }
    }

    private companion object {
        const val TAG = "ServerProbeDS"
    }
}
