package id.walt.services.velocitynetwork

import com.beust.klaxon.Klaxon
import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.model.DidVelocity
import id.walt.services.WaltIdServices
import id.walt.services.did.DidService
import id.walt.services.velocitynetwork.models.requests.*
import id.walt.services.velocitynetwork.models.responses.CreateOrganizationResponse
import id.walt.services.velocitynetwork.models.responses.DisclosureResponse
import id.walt.services.velocitynetwork.models.responses.ExchangeResponse
import id.walt.services.velocitynetwork.models.responses.OfferResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.pwall.json.schema.JSONSchema
import java.io.File

object VelocityClient {
    val registrarBearerTokenFile = File("${WaltIdServices.velocityDir}registrar-bearer-token.txt")
    val agentBearerTokenFile = File("${WaltIdServices.velocityDir}agent-bearer-token.txt")


    private val VELOCITY_NETWORK_ENV = System.getenv().get("VN_ENV") ?: "dev"
    private val VELOCITY_NETWORK_REGISTRAR_API = when (VELOCITY_NETWORK_ENV) {
        "prod" -> ""
        else -> VELOCITY_NETWORK_ENV
    }.let { String.format(VELOCITY_NETWORK_REGISTRAR_ENDPOINT, it) }

    const val VELOCITY_NETWORK_REGISTRAR_ENDPOINT = "https://%sregistrar.velocitynetwork.foundation/"
//    const val agentUrl = "https://devagent.velocitycareerlabs.io"
    const val agentUrl = "http://localhost:8080"
    const val exchangePath = "/api/holder/v0.6/org/%s/exchange"
    const val disclosurePath = "/api/holder/v0.6/org/%s/identify"
    const val offersPath = "/api/holder/v0.6/org/%s/issue/credential-offers"
    const val finalizePath = "/api/holder/v0.6/org/%s/issue/finalize-offers"
    const val registerOrganizationPath = "/api/v0.6/organizations/full"
    const val didResolvePath = "api/v0.6/resolve-did/%s"

    private val log = KotlinLogging.logger {}
    private val bearerTokenStorage = mutableListOf<BearerTokens>()

    val httpWithAuth = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(io.ktor.client.plugins.auth.Auth) {
            bearer {
                loadTokens {
                    bearerTokenStorage.last()
                }
            }
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.HEADERS
        }
    }

    fun registerOrganization(data: String, token: String) = runBlocking {
        log.debug { "Registering organization on Velocity Network... " }
        if (!validate(data)) throw Exception("Schema validation failed.")
        bearerTokenStorage.add(BearerTokens(token, token))
        httpWithAuth.post(VELOCITY_NETWORK_REGISTRAR_API + registerOrganizationPath) {
            setBody(data)
        }.bodyAsText().let { response ->
            Klaxon().parse<CreateOrganizationResponse>(response)?.let {
                DidService.importDidAndDoc(it.id, it.didDoc.encodePretty())
                File(WaltIdServices.velocityDir + it.id).writeText(response)
                it.id
            } ?: throw Exception("Error parsing response")
        }
    }

    //TODO: holder data - accept email instead of credential
    fun issue(holderIdentity: String, issuerDid: String, vararg credentialTypes: String): List<String> =
        runBlocking {
            // step 1: exchange id
            val exchangeId = WaltIdServices.http.post(agentUrl + exchangePath.format(issuerDid)) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(ExchangeRequestBody(ExchangeType.ISSUING.name))
            }.body<ExchangeResponse>()
            log.debug { "Using exchangeId ${exchangeId.exchangeId}" }
            // step 2: identification
            val token = WaltIdServices.http.post(agentUrl + disclosurePath.format(issuerDid)) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(DisclosureRequestBody(exchangeId.exchangeId, holderIdentity))
            }.body<DisclosureResponse>()
            bearerTokenStorage.add(BearerTokens(token.token, token.token))
            log.debug { "Using token ${token.token}" }
            // step 3: offers
            val offers = httpWithAuth.post(agentUrl + offersPath.format(issuerDid)) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(GetOffersRequestBody(exchangeId.exchangeId, credentialTypes.toList()))
            }.body<List<OfferResponse>>()
            log.debug { "Finalizing offers ${offers.map { it.id }}" }
            // step 4: get credential
            val credentials = httpWithAuth.post(agentUrl + finalizePath.format(issuerDid)) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(FinalizeOfferRequestBody(exchangeId.exchangeId, offers.map { it.id }, emptyList()))
            }.body<List<String>>()
            log.debug { "Credentials $credentials" }
            credentials
        }

    fun resolveDid(did: String) = runBlocking {
        val didUrl = DidUrl.from(did)
        log.debug { "Resolving DID ${didUrl.did}..." }
        bearerTokenStorage.add(BearerTokens(agentBearerTokenFile.readText(), ""))
        httpWithAuth.get(
            VELOCITY_NETWORK_REGISTRAR_ENDPOINT + didResolvePath.format(didUrl.did)
        ){
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }.body<Did>() as DidVelocity
    }

    private fun validate(data: String) =
        JSONSchema.parseFile("src/main/resources/velocitynetwork/schemas/organization-registration-reqSchema.json")
            .validateBasic(data).valid
}