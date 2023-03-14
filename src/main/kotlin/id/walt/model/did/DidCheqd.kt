package id.walt.model.did

import com.beust.klaxon.Json
import id.walt.model.Did
import id.walt.model.ServiceEndpoint
import id.walt.model.VerificationMethod

class DidCheqd(
    context: List<String> = listOf("https://www.w3.org/ns/did/v1"),
    id: String, // did:cheqd:mainnet:zF7rhDBfUt9d1gJPjx7s1JXfUY7oVWkY
    @Json(serializeNull = false) val service: List<ServiceEndpoint>? = null,
    //@SerialName("authentication") @JsonProperty("authentication") @Json(name = "authentication") val cheqdAuthentication: List<String>? = null,
    //@SerialName("_authentication") @JsonProperty("_authentication") @Json(name = "_authentication") override var authentication: List<VerificationMethod>? = null,
    authentication: List<VerificationMethod>? = null,
    verificationMethod: List<VerificationMethod>? = null,
    assertionMethod: List<VerificationMethod>? = null,
    capabilityDelegation: List<VerificationMethod>? = null,
    capabilityInvocation: List<VerificationMethod>? = null,
    keyAgreement: List<VerificationMethod>? = null,
    serviceEndpoint: List<ServiceEndpoint>? = null
) : Did(
    context = context,
    id = id,
    verificationMethod = verificationMethod,
    // authentication = emptyList(),
    authentication = authentication,
    assertionMethod = assertionMethod,
    capabilityDelegation = capabilityDelegation,
    capabilityInvocation = capabilityInvocation,
    keyAgreement = keyAgreement,
    serviceEndpoint = serviceEndpoint
) {

    constructor(
        context: String,
        id: String, // did:cheqd:mainnet:zF7rhDBfUt9d1gJPjx7s1JXfUY7oVWkY
        service: List<ServiceEndpoint>? = null,
        //authentication: List<String>? = null,
        authentication: List<VerificationMethod>? = null,
        verificationMethod: List<VerificationMethod>? = null,
        assertionMethod: List<VerificationMethod>? = null,
        capabilityDelegation: List<VerificationMethod>? = null,
        capabilityInvocation: List<VerificationMethod>? = null,
        keyAgreement: List<VerificationMethod>? = null,
        serviceEndpoint: List<ServiceEndpoint>? = null
    ) : this(
        context = listOf(context),
        id = id,
        service = service,
        authentication = authentication,
        verificationMethod = verificationMethod,
        assertionMethod = assertionMethod,
        capabilityDelegation = capabilityDelegation,
        capabilityInvocation = capabilityInvocation,
        keyAgreement = keyAgreement,
        serviceEndpoint = serviceEndpoint
    )

    override fun toString(): String {
        return "DidCheqd(context=$context, id=$id, verificationMethod=$verificationMethod, authentication=$authentication, service=$service)"
    }


}
