package id.walt.services.did.resolvers

import id.walt.model.Did
import id.walt.model.DidEbsi
import id.walt.model.DidUrl
import id.walt.model.DidVelocity
import id.walt.services.WaltIdServices
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class DidVelocityResolverImpl : DidResolverBase<DidVelocity>() {

    override fun resolve(did: String) = resolveDidVelocity(DidUrl.from(did))

    private fun resolveDidVelocity(didUrl: DidUrl): DidVelocity = runBlocking {

        log.debug { "Resolving DID ${didUrl.did}..." }

        var didDoc: String
        var lastEx: ClientRequestException? = null

        for (i in 1..5) {
            try {
                log.debug { "Resolving did:ebsi at: https://api.preprod.ebsi.eu/did-registry/v2/identifiers/${didUrl.did}" }
                didDoc =
                    WaltIdServices.http.get("https://api.preprod.ebsi.eu/did-registry/v2/identifiers/${didUrl.did}")
                        .bodyAsText()
                log.debug { "Result: $didDoc" }
                return@runBlocking Did.decode(didDoc)!! as DidVelocity
            } catch (e: ClientRequestException) {
                log.debug { "Resolving did ebsi failed: fail $i" }
                delay(1000)
                lastEx = e
            }
        }
        log.debug { "Could not resolve did ebsi!" }
        throw lastEx ?: Exception("Could not resolve did ebsi!")
    }

}