package org.letstrust.services.vc

import com.danubetech.keyformats.crypto.provider.Ed25519Provider
import com.danubetech.keyformats.crypto.provider.impl.TinkEd25519Provider
import foundation.identity.jsonld.ConfigurableDocumentLoader
import foundation.identity.jsonld.JsonLDObject
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.json.JSONObject
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.LetsTrustServices
import org.letstrust.crypto.SignatureType
import org.letstrust.crypto.LdSigner
import org.letstrust.model.*
import org.letstrust.crypto.keystore.KeyStore
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.streams.toList

private val log = KotlinLogging.logger {}

/**
 * W3C Verifiable Credential Service
 */
object CredentialService {

    private var ks: KeyStore = LetsTrustServices.load<KeyStore>()

    init {
        Ed25519Provider.set(TinkEd25519Provider())
    }

    fun sign(
        issuerDid: String,
        jsonCred: String,
        domain: String? = null,
        nonce: String? = null,
        verificationMethod: String? = null
    ): String {

        log.debug { "Signing jsonLd object with: issuerDid ($issuerDid), domain ($domain), nonce ($nonce)" }

        val jsonLdObject: JsonLDObject = JsonLDObject.fromJson(jsonCred)
        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader

        confLoader.isEnableHttp = true
        confLoader.isEnableHttps = true
        confLoader.isEnableFile = true
        confLoader.isEnableLocalCache = true
        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER

        val key = ks.load(issuerDid)

        val signer = when (key.algorithm) {
            KeyAlgorithm.ECDSA_Secp256k1 -> LdSigner.EcdsaSecp256k1Signature2019(key.keyId)
            KeyAlgorithm.EdDSA_Ed25519 -> LdSigner.Ed25519Signature2018(key.keyId)
            else -> throw Exception("Signature for key algorithm ${key.algorithm} not supported")
        }

        signer.creator = URI.create(issuerDid)
        signer.created = Date() // Use the current date
        signer.domain = domain
        signer.nonce = nonce
        verificationMethod?.let { signer.verificationMethod = URI.create(verificationMethod) }

        val proof = signer.sign(jsonLdObject)

        // TODO Fix: this hack is needed as, signature-ld encodes type-field as array, which is not correct
        // return correctProofStructure(proof, jsonCred)
        return jsonLdObject.toJson(true)

    }

    fun verifyVc(issuerDid: String, vc: String): Boolean {
        log.trace { "Loading verification key for:  $issuerDid" }

        val publicKey = ks.load(issuerDid)

        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader

        confLoader.isEnableHttp = true
        confLoader.isEnableHttps = true
        confLoader.isEnableFile = true
        confLoader.isEnableLocalCache = true

        log.trace { "Document loader config: isEnableHttp (${confLoader.isEnableHttp}), isEnableHttps (${confLoader.isEnableHttps}), isEnableFile (${confLoader.isEnableFile}), isEnableLocalCache (${confLoader.isEnableLocalCache})" }

        val jsonLdObject = JsonLDObject.fromJson(vc)
        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER
        log.trace { "Decoded Json LD object: $jsonLdObject" }

        val verifier = when (publicKey.algorithm) {
            KeyAlgorithm.ECDSA_Secp256k1 -> org.letstrust.crypto.LdVerifier.EcdsaSecp256k1Signature2019(publicKey.getPublicKey())
            KeyAlgorithm.EdDSA_Ed25519 -> org.letstrust.crypto.LdVerifier.Ed25519Signature2018(publicKey)
            else -> throw Exception("Signature for key algorithm ${publicKey.algorithm} not supported")
        }

        log.trace { "Loaded Json LD verifier with signature suite: ${verifier.signatureSuite}" }

        return verifier.verify(jsonLdObject)
    }


    //TODO: following methods might be depreciated


//    fun sign_old(
//        issuerDid: String,
//        jsonCred: String,
//        signatureType: SignatureType,
//        domain: String? = null,
//        nonce: String? = null
//    ): String {
//
//        log.debug { "Signing jsonLd object with: issuerDid ($issuerDid), signatureType ($signatureType), domain ($domain), nonce ($nonce)" }
//
//        val jsonLdObject: JsonLDObject = JsonLDObject.fromJson(jsonCred)
//        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader
//
//        confLoader.isEnableHttp = true
//        confLoader.isEnableHttps = true
//        confLoader.isEnableFile = true
//        confLoader.isEnableLocalCache = true
//        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER
//
//        val issuerKeys = KeyManagementService.loadKeys(issuerDid)
//        if (issuerKeys == null) {
//            log.error { "Could not load signing key for $issuerDid" }
//            throw Exception("Could not load signing key for $issuerDid")
//        }
//
//        val signer = when (signatureType) {
//            SignatureType.Ed25519Signature2018 -> Ed25519Signature2018LdSigner(issuerKeys.getPrivateAndPublicKey())
//            SignatureType.EcdsaSecp256k1Signature2019 -> EcdsaSecp256k1Signature2019LdSigner(ECKey.fromPrivate(issuerKeys.getPrivKey()))
//            SignatureType.Ed25519Signature2020 -> Ed25519Signature2020LdSigner(issuerKeys.getPrivateAndPublicKey())
//        }
//
//        signer.creator = URI.create(issuerDid)
//        signer.created = Date() // Use the current date
//        signer.domain = domain
//        signer.nonce = nonce
//
//        val proof = signer.sign(jsonLdObject)
//
//        // TODO Fix: this hack is needed as, signature-ld encodes type-field as array, which is not correct
//        // return correctProofStructure(proof, jsonCred)
//        return jsonLdObject.toJson(true)
//
//    }

    private fun correctProofStructure(ldProof: LdProof, jsonCred: String): String {
        val vc = Json.decodeFromString<VerifiableCredential>(jsonCred)
        vc.proof = Proof(
            ldProof.type,
            LocalDateTime.ofInstant(ldProof.created.toInstant(), ZoneId.systemDefault()),
            ldProof.creator.toString(),
            ldProof.proofPurpose,
            null,
            ldProof.proofValue,
            ldProof.jws
        )
        return Json.encodeToString(vc)
    }

    fun addProof(credMap: Map<String, String>, ldProof: LdProof): String {
        val signedCredMap = HashMap<String, Any>(credMap)
        signedCredMap["proof"] = JSONObject(ldProof.toJson())
        return JSONObject(signedCredMap).toString()
    }

    enum class VerificationType {
        VERIFIABLE_CREDENTIAL,
        VERIFIABLE_PRESENTATION
    }

    @Serializable
    data class VerificationResult(val verified: Boolean, val verificationType: VerificationType)

    fun verify(vcOrVp: String): VerificationResult {
        return when { // TODO: replace the raw contains call
            vcOrVp.contains("VerifiablePresentation") -> VerificationResult(verifyVp(vcOrVp), VerificationType.VERIFIABLE_PRESENTATION)
            else -> VerificationResult(verifyVc(vcOrVp), VerificationType.VERIFIABLE_CREDENTIAL)
        }

    }


    fun verifyVc(vc: String): Boolean {
        log.debug { "Verifying VC:\n$vc" }

        val vcObj = Json.decodeFromString<VerifiableCredential>(vc)
        log.trace { "VC decoded: $vcObj" }

        val signatureType = SignatureType.valueOf(vcObj.proof!!.type)
        log.debug { "Issuer: ${vcObj.issuer}" }
        log.debug { "Signature type: $signatureType" }

        val vcVerified = verifyVc(vcObj.issuer, vc)
        log.debug { "Verification of LD-Proof returned: $vcVerified" }
        return vcVerified
    }

    fun verifyVp(vp: String): Boolean {
        log.debug { "Verifying VP:\n$vp" }

        val vpObj = Json.decodeFromString<VerifiablePresentation>(vp)
        log.trace { "VC decoded: $vpObj" }

        val signatureType = SignatureType.valueOf(vpObj.proof!!.type)
        val issuer = vpObj.proof.creator!!
        log.debug { "Issuer: $issuer" }
        log.debug { "Signature type: $signatureType" }

        val vpVerified = verifyVc(issuer, vp)
        log.debug { "Verification of VP-Proof returned: $vpVerified" }

        val vc = vpObj.verifiableCredential[0]
        val vcStr = vc.encodePretty()
        log.debug { "Verifying VC:\n$vcStr" }
        val vcVerified = verifyVc(vc.issuer, vcStr)

        log.debug { "Verification of VC-Proof returned: $vpVerified" }

        return vpVerified && vcVerified
    }

//    fun verify_old(issuerDid: String, vc: String, signatureType: SignatureType): Boolean {
//        log.trace { "Loading verification key for:  $issuerDid" }
//        val issuerKeys = KeyManagementService.loadKeys(issuerDid)
//        if (issuerKeys == null) {
//            log.error { "Could not load verification key for $issuerDid" }
//            throw Exception("Could not load verification key for $issuerDid")
//        }
//
//        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader
//
//        confLoader.isEnableHttp = true
//        confLoader.isEnableHttps = true
//        confLoader.isEnableFile = true
//        confLoader.isEnableLocalCache = true
//
//        log.trace { "Document loader config: isEnableHttp (${confLoader.isEnableHttp}), isEnableHttps (${confLoader.isEnableHttps}), isEnableFile (${confLoader.isEnableFile}), isEnableLocalCache (${confLoader.isEnableLocalCache})" }
//
//        val jsonLdObject = JsonLDObject.fromJson(vc)
//        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER
//        log.trace { "Decoded Json LD object: $jsonLdObject" }
//
//        val verifier = when (signatureType) {
//            SignatureType.Ed25519Signature2018 -> Ed25519Signature2018LdVerifier(issuerKeys.getPubKey())
//            SignatureType.EcdsaSecp256k1Signature2019 -> EcdsaSecp256k1Signature2019LdVerifier(ECKey.fromPublicOnly(issuerKeys.getPubKey()))
//            SignatureType.Ed25519Signature2020 -> Ed25519Signature2020LdVerifier(issuerKeys.getPubKey())
//        }
//        log.trace { "Loaded Json LD verifier with signature suite: ${verifier.signatureSuite}" }
//
//        return verifier.verify(jsonLdObject)
//    }

    fun present(vc: String, domain: String?, challenge: String?): String {
        log.debug { "Creating a presentation for VC:\n$vc" }
        val vcObj = Json.decodeFromString<VerifiableCredential>(vc)
        log.trace { "Decoded VC $vcObj" }

        val holderDid = vcObj.credentialSubject.id ?: vcObj.credentialSubject.did ?: throw Exception("Could not determine holder DID for $vcObj")

        log.debug { "Holder DID: $holderDid" }

        val vpReq = VerifiablePresentation(listOf("https://www.w3.org/2018/credentials/v1"), "id", listOf("VerifiablePresentation"), listOf(vcObj), null)
        val vpReqStr = Json { prettyPrint = true }.encodeToString(vpReq)
        log.trace { "VP request:\n$vpReq" }

//        val holderKeys = KeyManagementService.loadKeys(holderDid!!)
//        if (holderKeys == null) {
//            log.error { "Could not load authentication key for $holderDid" }
//            throw Exception("Could not load authentication key for $holderDid")
//        }

        val vp = sign(holderDid, vpReqStr, domain, challenge)
        log.debug { "VP created:$vp" }
        return vp
    }

    fun listVCs(): List<String> {
        return Files.walk(Path.of("data/vc/created"))
            .filter { it -> Files.isRegularFile(it) }
            .filter { it -> it.toString().endsWith(".json") }
            .map { it.fileName.toString() }.toList()
    }

    fun defaultVcTemplate(): VerifiableCredential {
        return VerifiableCredential(
            listOf(
                "https://www.w3.org/2018/credentials/v1"
            ),
            "XXX",
            listOf("VerifiableCredential", "VerifiableAttestation"),
            "XXX",
            LocalDateTime.now().withNano(0),
            LocalDateTime.now().withNano(0),
            CredentialSubject(null, "XXX", null, listOf("claim1", "claim2")),
            CredentialStatus("https://essif.europa.eu/status", "CredentialsStatusList2020"),
            CredentialSchema("https://essif.europa.eu/tsr/education/CSR1224.json", "JsonSchemaValidator2018")
        )
    }
}
