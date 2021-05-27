package org.letstrust.crypto.keystore

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.crypto.SunCryptoService
import org.letstrust.services.key.KeyService
import kotlin.test.assertEquals

open class FileSystemKeyStoreTest {//: KeyStoreTest() {

    @Before
    fun setUp() {
        SunCryptoService.setKeyStore(FileSystemKeyStore)
        KeyService.setKeyStore(FileSystemKeyStore)
    }

    @After
    fun tearDown() {
        SunCryptoService.setKeyStore(SqlKeyStore)
        KeyService.setKeyStore(SqlKeyStore)
    }

    @Test
    fun listKeysTest() {

        var keyId1 = KeyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        var keyId2 = KeyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        var key1 = FileSystemKeyStore.load(keyId1.id)
        var key2 = FileSystemKeyStore.load(keyId2.id)
        assertEquals(keyId1, key1.keyId)
        assertEquals(keyId2, key2.keyId)
    }
}
