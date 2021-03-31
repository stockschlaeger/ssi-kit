package org.letstrust.crypto.keystore

import org.apache.commons.io.IOUtils
import org.letstrust.crypto.buildKey
import org.letstrust.crypto.Key
import org.letstrust.crypto.KeyId
import org.letstrust.crypto.toBase64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

object FileSystemKeyStore : KeyStore {

    //TODO: get path from config
    private const val KEY_DIR_PATH = "data/key"

    init {
        File(KEY_DIR_PATH).mkdirs()
    }

    override fun listKeys(): List<Key> {
        val keys = ArrayList<Key>()
        Files.walk(Paths.get(KEY_DIR_PATH))
            .filter { it -> Files.isRegularFile(it) }
            .filter { it -> it.toString().endsWith(".meta") }
            .forEach {
                val keyId = it.fileName.toString().substringBefore(".")
                load(keyId)?.let {
                    keys.add(it)
                }
            }
        return keys
    }

    override fun load(keyId: String): Key {
        val metaData = String(loadKeyFile(keyId, "meta"))
        val algorithm = metaData.substringBefore(delimiter = ";")
        val provider = metaData.substringAfter(delimiter = ";")
        val publicPart = File("$KEY_DIR_PATH/$keyId.enc-pubkey").readText()
        val privatePart = File("$KEY_DIR_PATH/$keyId.enc-privkey").readText()

        return buildKey(keyId, algorithm, provider, publicPart, privatePart)

    }

    override fun addAlias(keyId: KeyId, alias: String) {
        File("$KEY_DIR_PATH/Alias-$alias").writeText(keyId.id)
    }

    override fun store(key: Key) {
        addAlias(key.keyId, key.keyId.id)
        storeKeyMetaData(key)
        storePublicKeyPEM(key)
        storePrivateKeyWhenExistingPEM(key)

//        saveEncPublicKey(key.keyId.id, key.keyPair!!.public)
//        saveEncPrivateKey(key.keyId.id, key.keyPair!!.private)
    }

    override fun getKeyId(alias: String): String {
        return File("$KEY_DIR_PATH/Alias-$alias").readText()
    }

    override fun delete(alias: String) {
        deleteKeyFile(alias, "enc-pubkey")
        deleteKeyFile(alias, "enc-privkey")
        deleteKeyFile(alias, "raw-pubkey")
        deleteKeyFile(alias, "raw-privkey")
    }

    private fun storePublicKeyPEM(key: Key) {
        File("$KEY_DIR_PATH/${key.keyId.id}.enc-pubkey").writeText(key.getPublicKey().toBase64())
    }

    private fun storePrivateKeyWhenExistingPEM(key: Key) {
        if (key.keyPair != null && key.keyPair!!.private != null) {
            File("$KEY_DIR_PATH/${key.keyId.id}.enc-privkey").writeText(key.keyPair!!.private.toBase64())
        }
    }

    private fun storeKeyMetaData(key: Key) {
        saveKeyFile(key.keyId.id, "meta", (key.algorithm.name + ";" + key.cryptoProvider.name).toByteArray())
    }

    //TODO consider deprecated methods below


//    override fun saveKeyPair(keys: Keys) {
//        addAlias(keys.keyId, keys.keyId)
//        storeKeyMetaData(keys)
//
//        if (keys.isByteKey()) {
//            saveRawPublicKey(keys.keyId, keys.pair.public)
//            saveRawPrivateKey(keys.keyId, keys.pair.private)
//        } else {
//            saveEncPublicKey(keys.keyId, keys.pair.public)
//            saveEncPrivateKey(keys.keyId, keys.pair.private)
//        }
//    }

//    override fun load(keyId: String): Key {
//        val metaData = String(loadKeyFile(keyId, "meta"))
//        val algorithm = KeyAlgorithm.valueOf(metaData.substringBefore(delimiter = ";"))
//        val provider = CryptoProvider.valueOf(metaData.substringAfter(delimiter = ";"))
//
//
//        // KeyFactory.getInstance("RSA", "BC")
//        // KeyFactory.getInstance("ECDSA", "BC")
//
//        if (CryptoProvider.SUN.equals(provider)) {
//            val keyFactory = KeyFactory.getInstance(algorithm.name, provider.name)
//
//            if (keyFileExists(keyId, "enc-pubkey") && keyFileExists(keyId, "enc-privkey")) {
//                return Key(
//                    KeyId(keyId), algorithm, provider,
//                    KeyPair(loadEncPublicKey(keyId, keyFactory), loadEncPrivateKey(keyId, keyFactory))
//                )
//            }
////        } else {
////            if (keyFileExists(keyId, "raw-pubkey") && keyFileExists(keyId, "raw-privkey")) {
////                val keyPair = KeyPair(
////                    BytePublicKey(loadRawPublicKey(keyId), algorithm.name),
////                    BytePrivateKey(loadRawPrivateKey(keyId), algorithm.name)
////                )
////                return Key(KeyId(keyId), algorithm, provider, keyPair)
////            }
//        }
//        throw Exception("Could not load key: $keyId")
//    }


//   fun loadKeyPair(keyId: String): Key? {
//        val metaData = String(loadKeyFile(keyId, "meta"))
//        val algorithm = metaData.substringBefore(delimiter = ";")
//        val provider = metaData.substringAfter(delimiter = ";")
//        val publicPart = File("$KEY_DIR_PATH/$keyId.enc-pubkey").readText()
//        val privatePart = File("$KEY_DIR_PATH/$keyId.enc-privkey").readText()
//
//        return buildKey(keyId, algorithm,provider, publicPart, privatePart )
//
//        // KeyFactory.getInstance("RSA", "BC")
//        // KeyFactory.getInstance("ECDSA", "BC")
//
////        if (provider == "BC") {
////            val keyFactory = KeyFactory.getInstance(algorithm, provider)
////
////            if (keyFileExists(keyId, "enc-pubkey") && keyFileExists(keyId, "enc-privkey")) {
////                return Keys(
////                    keyId,
////                    KeyPair(loadEncPublicKey(keyId, keyFactory), loadEncPrivateKey(keyId, keyFactory)),
////                    provider
////                )
////            }
////        } else {
////            if (keyFileExists(keyId, "raw-pubkey") && keyFileExists(keyId, "raw-privkey")) {
////                val keyPair = KeyPair(
////                    BytePublicKey(loadRawPublicKey(keyId), algorithm),
////                    BytePrivateKey(loadRawPrivateKey(keyId), algorithm)
////                )
////                return Keys(keyId, keyPair, provider)
////            }
////        }
////        return null
//    }>

    private fun saveEncPublicKey(keyId: String, encodedPublicKey: PublicKey) =
        saveKeyFile(keyId, "enc-pubkey", X509EncodedKeySpec(encodedPublicKey.encoded).encoded)

    private fun saveEncPrivateKey(keyId: String, encodedPrivateKey: PrivateKey) =
        saveKeyFile(keyId, "enc-privkey", PKCS8EncodedKeySpec(encodedPrivateKey.encoded).encoded)

    private fun saveRawPublicKey(keyId: String, rawPublicKey: PublicKey) =
        saveKeyFile(keyId, "raw-pubkey", rawPublicKey.encoded)

    private fun saveRawPrivateKey(keyId: String, rawPrivateKey: PrivateKey) =
        saveKeyFile(keyId, "raw-privkey", rawPrivateKey.encoded)

    private fun saveKeyFile(keyId: String, suffix: String, data: ByteArray): Unit =
        FileOutputStream("$KEY_DIR_PATH/$keyId.$suffix").use { it.write(data) }

    private fun loadKeyFile(keyId: String, suffix: String): ByteArray =
        IOUtils.toByteArray(FileInputStream("$KEY_DIR_PATH/$keyId.$suffix"))

    private fun deleteKeyFile(keyId: String, suffix: String) = File("$KEY_DIR_PATH/$keyId.$suffix").delete()

    fun getKeyIdList() = File(KEY_DIR_PATH).listFiles()!!.map { it.nameWithoutExtension }.distinct()

    private fun keyFileExists(keyId: String, suffix: String) = File("$KEY_DIR_PATH/$keyId.$suffix").exists()

    private fun loadRawPublicKey(keyId: String): ByteArray = loadKeyFile(keyId, "raw-pubkey")

    private fun loadRawPrivateKey(keyId: String): ByteArray = loadKeyFile(keyId, "raw-privkey")

    private fun loadEncPublicKey(keyId: String, keyFactory: KeyFactory): PublicKey {
        return keyFactory.generatePublic(
            X509EncodedKeySpec(
                loadKeyFile(keyId, "enc-pubkey")
            )
        )
    }

    private fun loadEncPrivateKey(keyId: String, keyFactory: KeyFactory): PrivateKey {
        return keyFactory.generatePrivate(
            PKCS8EncodedKeySpec(
                loadKeyFile(keyId, "enc-privkey")
            )
        )
    }

}
