package org.letstrust.crypto

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.Curve
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.signer.LdSigner
import info.weboftrust.ldsignatures.suites.EcdsaSecp256k1Signature2019SignatureSuite
import info.weboftrust.ldsignatures.suites.SignatureSuites
import info.weboftrust.ldsignatures.util.JWSUtil

class EcdsaSecp256k1Signature2019LdSigner(val keyId: String) :
    LdSigner<EcdsaSecp256k1Signature2019SignatureSuite?>(SignatureSuites.SIGNATURE_SUITE_ECDSASECP256L1SIGNATURE2019, null) {

    override fun sign(ldProofBuilder: LdProof.Builder<*>, signingInput: ByteArray) {
        val jwsHeader = JWSHeader.Builder(JWSAlgorithm.ES256K).base64URLEncodePayload(false).criticalParams(setOf("b64")).build()
        val jwsSigningInput = JWSUtil.getJwsSigningInput(jwsHeader, signingInput)
        val jwsSigner = ECDSASigner(PrivateKeyHandle(keyId), Curve.SECP256K1)
        jwsSigner.jcaContext.provider = LetsTrustProvider()
        val signature = jwsSigner.sign(jwsHeader, jwsSigningInput)
        val jws = JWSUtil.serializeDetachedJws(jwsHeader, signature)
        ldProofBuilder.jws(jws)
    }
}