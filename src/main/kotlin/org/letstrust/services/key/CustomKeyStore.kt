package org.letstrust.services.key

import org.letstrust.crypto.Key
import org.letstrust.crypto.KeyId


class CustomKeyStore constructor() : KeyStore {

    init {

    }

    fun generate() {

    }

    override fun getKeyId(keyId: String): String? {
        TODO("Not yet implemented")
    }

    override fun saveKeyPair(keys: Keys) {
        TODO("Not yet implemented")
    }

    override fun listKeys(): List<Keys> {
        TODO("Not yet implemented")
    }

    override fun loadKeyPair(keyId: String): Keys? {
        TODO("Not yet implemented")
    }

    override fun delete(alias: String) {
        TODO("Not yet implemented")
    }

    override fun addAlias(keyId: String, alias: String) {
        TODO("Not yet implemented")
    }

    override fun store(key: Key) {
        TODO("Not yet implemented")
    }

    override fun load(alias: String): Key {
        TODO("Not yet implemented")
    }

    override fun addAlias(keyId: KeyId, alias: String) {
        TODO("Not yet implemented")
    }
}
