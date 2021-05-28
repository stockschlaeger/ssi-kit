package org.letstrust.common

import com.nimbusds.jose.util.Base64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Before
import org.junit.Test
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.services.key.KeyService
import java.security.Security
import kotlin.test.assertEquals

class SQLiteTest {
    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun createKeyStoreDb() {

        val kms = KeyService
        val keyId = kms.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val key = kms.load(keyId.id, true)!!
        val db = SqlDbManager
        val pubKeyStr = Base64.encode(key.getPublicKey().encoded).toString()


        db.getConnection().use { con ->
            con.createStatement().use { stmt ->
                stmt.executeUpdate("drop table if exists lt_keystore")
                stmt.executeUpdate(
                    "create table if not exists lt_keystore(" +
                            "id integer primary key autoincrement, " +
                            "name string unique, " +
                            "key string)"
                )
                stmt.executeQuery("select * from lt_keystore").use { rs ->
                    while (rs.next()) {
                        // read the result set
                        println("name = " + rs.getString("name"))
                        println("key = " + rs.getString("key"))
                        val keyStr = rs.getString("key")
                        println("id = " + rs.getInt("id"))
                        assertEquals(pubKeyStr, keyStr)
                        var key = Base64.from(keyStr).decode()
                    }
                }
                stmt.executeQuery("SELECT last_insert_rowid() AS lastRowId").use { rs ->
                    println(rs.next())
                    println("lastRowId: " + rs.getInt("lastRowId"))
                }
            }
        }
    }
}
