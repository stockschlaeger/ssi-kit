import org.apache.commons.codec.binary.Hex
import org.bitcoinj.core.Base58
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Before
import org.junit.Test
import java.security.KeyFactory
import java.security.Security
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class KeyManagementServiceTest {

    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun checkRequiredAlgorithms() {
        val kms = KeyManagementService
        var secp256k1 = false
        var P521 = false
        kms.getSupportedCurveNames().forEach {
            // println(it)
            if ("secp256k1".equals(it)) {
                secp256k1 = true
            } else if ("P-521".equals(it)) {
                P521 = true
            }
        }
        assertTrue(secp256k1)
        assertTrue(P521)
    }

    @Test
    fun generateSecp256k1KeyPairTest() {
        val kms = KeyManagementService
        val keyId = kms.generateEcKeyPair("secp256k1")
        val keysLoaded = kms.loadKeys(keyId)
        assertEquals(keyId, keysLoaded?.keyId)
        assertNotNull(keysLoaded?.pair)
        assertNotNull(keysLoaded?.pair?.private)
        assertNotNull(keysLoaded?.pair?.public)
        assertEquals("ECDSA", keysLoaded?.pair?.private?.algorithm)
        kms.deleteKeys(keyId)
    }

    @Test
    fun generateEd25519KeyPairTest() {
        val kms = KeyManagementService
        val keyId = kms.generateKeyPair("Ed25519")
        val keysLoaded = kms.loadKeys(keyId)
        assertEquals(keyId, keysLoaded?.keyId)
        assertNotNull(keysLoaded?.pair?.private?.encoded)
        assertNotNull(keysLoaded?.pair?.public?.encoded)
        var pubKey = keysLoaded?.pair?.public?.encoded
        assertEquals(32, pubKey?.size)
        assertTrue(kms.getMultiBase58PublicKey(keyId).length > 32)
        kms.deleteKeys(keyId)
    }

    @Test
    fun generateRsaKeyPairTest() {
        val kms = KeyManagementService
        val ks = FileSystemKeyStore
        val keyId = kms.generateKeyPair("RSA")
        val keysLoaded = kms.loadKeys(keyId)
        assertEquals(keyId, keysLoaded?.keyId)
        assertNotNull(keysLoaded?.pair)
        assertNotNull(keysLoaded?.pair?.private)
        assertNotNull(keysLoaded?.pair?.public)
        assertEquals("RSA", keysLoaded?.pair?.private?.algorithm)
        kms.deleteKeys(keyId)
    }

}
