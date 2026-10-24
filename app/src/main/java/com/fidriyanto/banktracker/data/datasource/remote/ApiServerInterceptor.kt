package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.StorageSettingsDataSource
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Sends every API call to the server the user chose, with its token. The Retrofit services are built
// with a placeholder address, so the address can change at run time, for example during setup.
@Singleton
class ApiServerInterceptor @Inject constructor(
    private val storage: StorageSettingsDataSource,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val server = storage.current().server ?: throw IOException("No FinTrack server is set up")
        val base = server.baseUrl.toHttpUrlOrNull() ?: throw IOException("The server address is not valid")
        val request = chain.request()
        val url = request.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .encodedPath(base.encodedPath.trimEnd('/') + request.url.encodedPath)
            .build()
        return chain.proceed(request.newBuilder().url(url).header("Authorization", "Bearer ${server.token}").build())
    }
}
