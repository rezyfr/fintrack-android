package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.domain.model.ServerCheck
import com.fidriyanto.banktracker.domain.model.ServerConfig
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.UnknownServiceException

class ServerProbeRemoteDataSourceImplTest {
    private val service = mockk<ServerProbeApiService>()
    private val probe = ServerProbeRemoteDataSourceImpl(service)

    private fun check(url: String) = runBlocking { probe.check(ServerConfig(url, "test-token")) }

    // Android blocks a plain http:// call, so setup must say that the address needs https://.
    @Test fun aBlockedHttpAddressNeedsHttps() {
        coEvery { service.health(any()) } throws UnknownServiceException("CLEARTEXT communication not permitted")
        assertEquals(ServerCheck.NeedsHttps, check("http://192.168.1.10:8080"))
    }

    @Test fun aServerThatDoesNotAnswerIsUnreachable() {
        coEvery { service.health(any()) } throws IOException("timeout")
        assertEquals(ServerCheck.Unreachable, check("https://fintrack.example.com"))
    }

    @Test fun anAddressOkHttpCannotParseIsABadAddress() {
        coEvery { service.health(any()) } throws IllegalArgumentException("bad url")
        assertEquals(ServerCheck.BadAddress, check("https://bad host"))
    }
}
