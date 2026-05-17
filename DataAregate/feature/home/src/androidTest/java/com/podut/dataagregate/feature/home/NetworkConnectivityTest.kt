package com.podut.dataagregate.feature.home

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkConnectivityTest {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    @Test
    fun testConnectionToHostDocker() = runBlocking {
        // IP-ul hostului tau detectat: 192.168.0.126
        val url = "http://192.168.0.126:8085/health"
        
        try {
            val response: HttpResponse = client.get(url)
            println("Test Conexiune - Status: ${response.status}")
            assertTrue("Microserviciul Docker nu raspunde pe $url. Status: ${response.status}", response.status.value == 200)
        } catch (e: Exception) {
            assertTrue("Eroare de conexiune catre $url: ${e.message}", false)
        }
    }
}
