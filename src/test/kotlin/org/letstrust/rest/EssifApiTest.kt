package org.letstrust.rest

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.letstrust.services.essif.EnterpriseWalletService
import org.letstrust.services.essif.EosService
import kotlin.test.assertEquals

class EssifApiTest {

    val ESSIF_API_URL = "http://localhost:7002"

    val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

    fun get(path: String): HttpResponse = runBlocking {
        val response: HttpResponse = client.get("$ESSIF_API_URL$path") {
            headers {
                append(HttpHeaders.Accept, "text/html")
                append(HttpHeaders.Authorization, "token")
            }
        }
        assertEquals(200, response.status.value)
        return@runBlocking response
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun startServer() {
            RestAPI.startEssifApi(7002)
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            RestAPI.stopEssifApi()
        }
    }

    @Test
    fun testHealth() = runBlocking {
        val response = get("/health")
        assertEquals("OK", response.readText())
    }

    @Test
    fun testOnboarding() = runBlocking {
        println("ESSIF onboarding of a Legal Entity by requesting a Verifiable ID")

        val credentialRequestUri = EosService.requestCredentialUri()

        val didOwnershipReq = EnterpriseWalletService.requestVerifiableCredential(credentialRequestUri)

        val didOfLegalEntity = EnterpriseWalletService.createDid()

        val verifiableId = EnterpriseWalletService.getVerifiableCredential(didOwnershipReq, didOfLegalEntity)
    }
}
