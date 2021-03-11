import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.Ed25519Signer
import com.nimbusds.jose.crypto.Ed25519Verifier
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test
import java.util.*
import com.nimbusds.jose.jwk.gen.ECKeyGenerator

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import java.security.*;
import java.security.interfaces.*;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.*;
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.OctetKeyPair
import org.letstrust.JwtService
import org.letstrust.KeyManagementService


// https://mkjwk.org/
// https://bitbucket.org/connect2id/nimbus-jose-jwt/src/master/
// https://github.com/mitreid-connect/json-web-key-generator
@Deprecated(message = "This is a test-class and should be removed")
class JwtTest {


    @Test // https://connect2id.com/products/nimbus-jose-jwt/examples/jws-with-eddsa
    fun jwtEd25519() {
        // Generate a key pair with Ed25519 curve
        val jwk = OctetKeyPairGenerator(Curve.Ed25519)
            .keyID("123")
            .generate()
        val publicJWK = jwk.toPublicJWK()

        // Create the EdDSA signer
        val signer: JWSSigner = Ed25519Signer(jwk)

        // Prepare JWT with claims set
        val claimsSet = JWTClaimsSet.Builder()
            .subject("alice")
            .issuer("https://c2id.com")
            .expirationTime(Date(Date().getTime() + 60 * 1000))
            .build()

        var signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.EdDSA).keyID(jwk.keyID).build(),
            claimsSet
        )

        // Compute the EC signature
        signedJWT.sign(signer)

        // Serialize the JWS to compact form
        val s = signedJWT.serialize()

        // On the consumer side, parse the JWS and verify its EdDSA signature
        signedJWT = SignedJWT.parse(s)

        println(s)
        println(publicJWK.toJSONString())
        println(jwk.toJSONString())
        val verifier: JWSVerifier = Ed25519Verifier(publicJWK)
        assertTrue(signedJWT.verify(verifier))

        // Retrieve / verify the JWT claims according to the app requirements
        assertEquals("alice", signedJWT.jwtClaimsSet.subject)
        assertEquals("https://c2id.com", signedJWT.jwtClaimsSet.issuer)
        assertTrue(Date().before(signedJWT.jwtClaimsSet.expirationTime))
    }

    @Test
    fun jwtSpec256k1() {
        // Generate EC key pair on the secp256k1 curve
        val ecJWK = ECKeyGenerator(Curve.SECP256K1)
            .keyUse(KeyUse.SIGNATURE)
            .keyID("123")
            .generate()

        // Get the public EC key, for recipients to validate the signatures

        val ecPublicJWK = ecJWK.toPublicJWK()

        // Sample JWT claims
        val claimsSet = JWTClaimsSet.Builder()
            .subject("alice")
            .build()

        // Create JWT for ES256K alg
        val jwt = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.ES256K)
                .keyID(ecJWK.keyID)
                .build(),
            claimsSet
        )

        // Sign with private EC key


        jwt.sign(ECDSASigner(ecJWK))

        // Output the JWT
        println(jwt.serialize())

        // With line break for clarity:
        // eyJraWQiOiIxMjMiLCJhbGciOiJFUzI1NksifQ.eyJzdWIiOiJhbGljZSJ9
        // .zRQyjdmePW97V5JYbPxwOrrtL0MdDPuz7w9O0CWvF-U40g195qBuZ8fXH2
        // XZi_-U4RdMr4JvbiTKXH1ClofZgw

        // Parse the signed JWT
        val jwtString = "eyJraWQiOiIxMjMiLCJhbGciOiJFUzI1NksifQ.eyJzdWIiOiJhbGljZSJ9.kbqs7pDjDCKNsy9KddEnfOD0i1BjsiqveVJztAXraIsH_ATLzJ_LqyDSTQ0vQhfuy24HBdA4jSwax8_0r6gTbg"

        val jwt2 = SignedJWT.parse(jwt.serialize())

        // Verify the ES256K signature with the public EC key

        assertTrue(jwt2.verify(ECDSAVerifier(ecPublicJWK)))

        // Output the JWT claims: {"sub":"alice"}

        // Output the JWT claims: {"sub":"alice"}
        println(jwt2.jwtClaimsSet.toJSONObject())
    }

    // https://github.com/felx/nimbus-jose-jwt/blob/master/src/test/java/com/nimbusds/jose/crypto/ECDHCryptoTest.java
    @Test
    fun signAndEncryptedJwtP_256() {
        // setup
        // Generate EC key pair on the secp256k1 curve
        val senderJWK = ECKeyGenerator(Curve.SECP256K1)
            .keyUse(KeyUse.SIGNATURE)
            .keyID("123")
            .generate()

        // Create JWT
        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.ES256K).keyID(senderJWK.getKeyID()).build(),
            JWTClaimsSet.Builder()
                .subject("test")
                .issueTime(Date())
                .issuer("https://test.com")
                .build()
        )

        // Sign the JWT
        signedJWT.sign(ECDSASigner(senderJWK))

        // Create JWE object with signed JWT as payload
        val jweObject = JWEObject(
            JWEHeader.Builder(JWEAlgorithm.ECDH_ES, EncryptionMethod.A256GCM)
                .contentType("JWT") // required to indicate nested JWT
                .build(),
            Payload(signedJWT)
        )

        val recipientPublicJWK = ECKeyGenerator(Curve.P_256) // SECP256K1 not working for encrypter
            .keyUse(KeyUse.SIGNATURE)
            .keyID("456")
            .generate()

        // Encrypt with the recipient's public key
        jweObject.encrypt(ECDHEncrypter(recipientPublicJWK))

        // Serialise to JWE compact form
        val jweString = jweObject.serialize()

        println(jweString)

        var jwe2 = JWEObject.parse(jweString)

        jwe2.decrypt(ECDHDecrypter(recipientPublicJWK))

        val jwt2 = jwe2.payload.toSignedJWT()

        // Verify the ES256K signature with the public EC key

        assertTrue(jwt2.verify(ECDSAVerifier(senderJWK)))
    }

    @Test
    fun signAndEncryptedJwtEd25519() {

        // setup
        // Generate a key pair with Ed25519 curve
        val senderJWK = OctetKeyPairGenerator(Curve.Ed25519)
            .keyID("123")
            .generate()
        val senderPublicJWK = senderJWK.toPublicJWK()

        // Create the EdDSA signer
        val signer: JWSSigner = Ed25519Signer(senderJWK)

        // Prepare JWT with claims set
        val claimsSet = JWTClaimsSet.Builder()
            .subject("test")
            .issuer("https://test.com")
            .build()

        var signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.EdDSA).keyID(senderJWK.keyID).build(),
            claimsSet
        )

        // Compute the EC signature
        signedJWT.sign(signer)

        // Create JWE object with signed JWT as payload
        val jweObject = JWEObject(
            JWEHeader.Builder(JWEAlgorithm.ECDH_ES, EncryptionMethod.A256GCM)
                .contentType("JWT") // required to indicate nested JWT
                .build(),
            Payload(signedJWT)
        )

        val recipientJWK = OctetKeyPairGenerator(Curve.X25519)
            .keyID("123")
            .generate()
        val recipientPublicJWK = recipientJWK.toPublicJWK()

        // Encrypt with the recipient's public key
        jweObject.encrypt(X25519Encrypter(recipientPublicJWK))

        // Serialise to JWE compact form
        val jweString = jweObject.serialize()

        println(jweString)

        var jwe2 = JWEObject.parse(jweString)

        jwe2.decrypt(X25519Decrypter(recipientJWK))

        val jwt2 = jwe2.payload.toSignedJWT()

        // Verify the Ed25519 signature with the public EC key
       assertTrue(jwt2.verify(Ed25519Verifier(senderPublicJWK)))
    }
}
