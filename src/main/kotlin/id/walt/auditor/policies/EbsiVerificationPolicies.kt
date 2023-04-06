package id.walt.auditor.policies

import id.walt.auditor.ParameterizedVerificationPolicy
import id.walt.auditor.SimpleVerificationPolicy
import id.walt.auditor.VerificationPolicyResult
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.VerifiablePresentation
import id.walt.model.TrustedIssuer
import id.walt.model.TrustedIssuerType
import id.walt.services.WaltIdServices
import id.walt.services.did.DidService
import id.walt.services.ecosystems.essif.TrustedIssuerClient
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class EbsiTrustedSchemaRegistryPolicy : SimpleVerificationPolicy() {

    override val description: String = "Verify by EBSI Trusted Schema Registry"
    private val httpClient = WaltIdServices.httpNoAuth

    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {

        try {

            if (vc is VerifiablePresentation)
                return VerificationPolicyResult.success()

            val credentialSchemaUrl = vc.credentialSchema?.id?.takeIf { it.isNotEmpty() }
                ?: return VerificationPolicyResult.failure(IllegalArgumentException("Credential has no associated credentialSchema property"))

            if (!credentialSchemaUrl.startsWith("${TrustedIssuerClient.domain}/${TrustedIssuerClient.trustedSchemaPath}")) {
                return VerificationPolicyResult.failure(Throwable("No valid EBSI Trusted Schema Registry URL"))
            }

            if (runBlocking { httpClient.get(credentialSchemaUrl).status != HttpStatusCode.OK }) {
                return VerificationPolicyResult.failure(Throwable("Schema not available in the EBSI Trusted Schema Registry"))
            }

        } catch (e: Exception) {
            VerificationPolicyResult.failure(e)
        }

        return VerificationPolicyResult.success()
    }
}

class EbsiTrustedIssuerDidPolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by trusted issuer did"
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        return try {
            VerificationPolicyResult(DidService.loadOrResolveAnyDid(vc.issuerId!!) != null)
        } catch (e: ClientRequestException) {
            VerificationPolicyResult.failure(
                IllegalArgumentException(
                    when {
                        "did must be a valid DID" in e.message -> "did must be a valid DID"
                        "Identifier Not Found" in e.message -> "Identifier Not Found"
                        else -> throw e
                    }
                )
            )
        }
    }
}

data class EbsiTrustedIssuerRegistryPolicyArg(
    val registryAddress: String,
    val issuerType: TrustedIssuerType,
)

class EbsiTrustedIssuerRegistryPolicy(registryArg: EbsiTrustedIssuerRegistryPolicyArg) :
    ParameterizedVerificationPolicy<EbsiTrustedIssuerRegistryPolicyArg>(registryArg) {

    constructor(registryAddress: String, issuerType: TrustedIssuerType) : this(
        EbsiTrustedIssuerRegistryPolicyArg(registryAddress, issuerType)
    )
    constructor(issuerType: TrustedIssuerType) : this(
        "${TrustedIssuerClient.domain}/${TrustedIssuerClient.trustedIssuerPath}",
        issuerType
    )

    constructor(registryAddress: String) : this(registryAddress, TrustedIssuerType.Undefined)

    constructor() : this(
        EbsiTrustedIssuerRegistryPolicyArg(
            "${TrustedIssuerClient.domain}/${TrustedIssuerClient.trustedIssuerPath}",
            TrustedIssuerType.Undefined
        )
    )

    override val description: String = "Verify by an EBSI Trusted Issuers Registry compliant api."
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {

        // VPs are not considered
        if (vc is VerifiablePresentation)
            return VerificationPolicyResult.success()

        val issuerDid = vc.issuerId!!

        val resolvedIssuerDid = DidService.loadOrResolveAnyDid(issuerDid)
            ?: throw IllegalArgumentException("Could not resolve issuer DID $issuerDid")

        if (resolvedIssuerDid.id != issuerDid) {
            return VerificationPolicyResult.failure(IllegalArgumentException("Resolved DID ${resolvedIssuerDid.id} does not match the issuer DID $issuerDid"))
        }
        var tirRecord: TrustedIssuer


        return VerificationPolicyResult(runCatching {
            tirRecord = TrustedIssuerClient.getIssuer(issuerDid, argument.registryAddress)
            isValidTrustedIssuerRecord(tirRecord)
        }.getOrElse {
            log.debug { it }
            log.warn { "Could not resolve issuer TIR record of $issuerDid" }
            false
        })
    }

    private fun isValidTrustedIssuerRecord(tirRecord: TrustedIssuer): Boolean = tirRecord.attributes.any {
        it.issuerType.equals(argument.issuerType.name, ignoreCase = true)
    }

    override var applyToVP: Boolean = false
}


class EbsiTrustedSubjectDidPolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by trusted subject did"
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        return VerificationPolicyResult(vc.subjectId?.let {
            if (it.isEmpty()) true
            else try {
                DidService.loadOrResolveAnyDid(it) != null
            } catch (e: ClientRequestException) {
                if (!e.message.contains("did must be a valid DID") && !e.message.contains("Identifier Not Found")) throw e
                false
            }
        } ?: false)
    }
}

class EbsiTrustedIssuerAuthorizationClaimsPolicy : SimpleVerificationPolicy() {
    override val description: String
        get() = "Verify by issuer's authorized claims"

    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        // get accreditations of the issuers
        val tirRecords = (vc.properties["termsOfUse"] as? List<HashMap<String, String>>)?.filter {
            it["type"] == "VerifiableAccreditation"
        }?.mapNotNull {
            // fetch the issuers registry attributes
            runCatching { TrustedIssuerClient.getIssuer("", it["id"]!!) }.getOrNull()
        } ?: emptyList()

        // check the credential schema to match the issuers' authorized schemas
        return tirRecords.any {
            it.attributes.any {
                // attribute's body field holds the credential as jwt
                (VerifiableCredential.fromString(it.body).credentialSubject?.properties?.get("authorisationClaims") as? List<HashMap<String, String>>)?.any {
                    it["authorisedSchemaId"] == vc.credentialSchema?.id
                } ?: false
            }
        }.takeIf { it }?.let { VerificationPolicyResult.success() } ?: VerificationPolicyResult.failure()
    }
}
