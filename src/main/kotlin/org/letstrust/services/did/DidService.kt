package org.letstrust.services.did

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.bouncycastle.asn1.ASN1BitString
import org.bouncycastle.asn1.ASN1Sequence
import org.letstrust.*
import org.letstrust.crypto.CryptoService
import org.letstrust.model.*
import org.letstrust.services.key.KeyManagementService
import org.letstrust.services.key.KeyStore
import org.letstrust.services.key.Keys
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.streams.toList

private val log = KotlinLogging.logger {}

/**
 * W3C Decentralized Identity Service
 */
object DidService {

    private val crypto = LetsTrustServices.load<CryptoService>()
    private val ks: KeyStore = LetsTrustServices.load<KeyStore>()

    // Public methods

    fun create(method: DidMethod): String {
        return when (method) {
            DidMethod.key -> createDidKey()
            DidMethod.web -> createDidWeb()
            else -> throw Exception("DID method $method not supported")
        }
    }

    fun resolve(did: String): Did = resolve(did.toDidUrl())
    fun resolve(didUrl: DidUrl): Did {
        return when (didUrl.method) {
            DidMethod.key.name -> resolveDidKey(didUrl)
            DidMethod.web.name -> resolveDidWebDummy(didUrl)
            else -> TODO("did:${didUrl.method} not implemented yet")
        }
    }

    // Private methods

    private fun createDidKey(): String {
        val keyId = crypto.generateKey(KeyAlgorithm.Ed25519)
        val key = ks.load(keyId)

        val pubPrim = ASN1Sequence.fromByteArray(key.keyPair!!.public.encoded) as ASN1Sequence
        val x = (pubPrim.getObjectAt(1) as ASN1BitString).octets

        val identifier = convertEd25519PublicKeyToMultiBase58Btc(x)

        return "did:key:$identifier"
    }

    private fun createDidWeb(): String {
        TODO("")
    }

    private fun resolveDidKey(didUrl: DidUrl): Did {
        val pubKey = convertEd25519PublicKeyFromMultibase58Btc(didUrl.identifier)
        return ed25519Did(didUrl, pubKey)
    }

    private fun ed25519Did(didUrl: DidUrl, pubKey: ByteArray): Did {

        val dhKey = convertPublicKeyEd25519ToCurve25519(pubKey)

        val dhKeyMb = convertX25519PublicKeyToMultiBase58Btc(dhKey)

        val pubKeyId = didUrl.identifier + "#" + didUrl.identifier
        val dhKeyId = didUrl.identifier + "#" + dhKeyMb

        val verificationMethods = listOf(
            VerificationMethod(pubKeyId, "Ed25519VerificationKey2018", didUrl.did, pubKey.encodeBase58()),
            VerificationMethod(dhKeyId, "X25519KeyAgreementKey2019", didUrl.did, dhKey.encodeBase58())
        )

        val keyRef = listOf(pubKeyId)

        return Did(
            DID_CONTEXT_URL,
            didUrl.did,
            verificationMethods,
            keyRef,
            keyRef,
            keyRef,
            keyRef,
            listOf(dhKeyId),
            null
        )
    }








    // TODO: consider the methods below. They might be deprecated!

    fun resolveDid(did: String): Did = resolveDid(did.toDidUrl())

    fun resolveDid(didUrl: DidUrl): Did {
        return when (didUrl.method) {
            "key" -> resolveDidKey(didUrl)
            "web" -> resolveDidWebDummy(didUrl)
            else -> TODO("did:${didUrl.method} implemented yet")
        }
    }



    private fun resolveDidWebDummy(didUrl: DidUrl): Did {
        KeyManagementService.loadKeys(didUrl.did).let {
            return ed25519Did(didUrl, it!!.getPubKey())
        }
    }

    internal fun resolveDidWeb(didUrl: DidUrl): DidWeb {
        var domain = didUrl.identifier
        var didUrl = "https://${domain}/.well-known/did.json"
        log.debug { "Resolving did:web for domain $domain at: $didUrl" }
        var didWebStr = URL(didUrl).readText()
        log.debug { "did:web resolved:\n$didWebStr" }
        print(didWebStr)
        var did = Json.decodeFromString<DidWeb>(didWebStr)
        log.debug { "did:web decoded:\n$did" }
        return did
    }

    fun createDid(didMethod: String, keys: Keys? = null): String {

        val didKey = if (keys != null) keys else {
            val keyId = KeyManagementService.generateEd25519KeyPairNimbus() // .generateKeyPair("Ed25519")
            KeyManagementService.loadKeys(keyId)
        }!!

        return when (didMethod) {
            "key" -> createDidKey(didKey)
            "web" -> createDidWeb(didKey)
            else -> TODO("did creation by method $didMethod not supported yet")
        }
    }

    internal fun createDidKey(didKey: Keys): String {

        val identifier = convertEd25519PublicKeyToMultiBase58Btc(didKey.getPubKey())

        val did = "did:key:$identifier"

        KeyManagementService.addAlias(didKey.keyId, did)

        return did
    }

    internal fun createDidWeb(didKey: Keys): String {
        val domain = "letstrust.org"
        val username = UUID.randomUUID().toString().replace("-", "")
        val path = ":user:$username"

        val didUrl = DidUrl("web", "" + domain + path, didKey.keyId)

        KeyManagementService.addAlias(didKey.keyId, didUrl.did)

        return didUrl.did
    }

    fun listDids(): List<String> {

        // File("data").walkTopDown().filter {  it -> Files.isRegularFile(it)  }

        return Files.walk(Path.of("data/did/created"))
            .filter { it -> Files.isRegularFile(it) }
            .filter { it -> it.toString().endsWith(".json") }
            .map { it.fileName.toString() }.toList()
    }



}
