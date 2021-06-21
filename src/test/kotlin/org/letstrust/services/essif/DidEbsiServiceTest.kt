package org.letstrust.services.essif

import org.junit.Test
import org.letstrust.LetsTrustServices
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.crypto.buildKey
import org.letstrust.crypto.toPEM
import org.letstrust.services.key.KeyService
import org.web3j.crypto.ECKeyPair
import java.security.KeyPair
import java.security.PublicKey
import kotlin.test.assertEquals

class DidEbsiServiceTest {
    init {
        // For BouncyCastle
        LetsTrustServices
    }

    @Test
    fun testSignTransaction() {
        val ecKeyPair = getKeyPair()
        val unsignedTransaction = getUnsignedTx()
        val signedTx = DidEbsiService.signTransaction(ecKeyPair, unsignedTransaction)
        assertEquals("0x33ba924eaca0b6f3b0e21de371c15d0f2851432a536432bc2d3160ad17d78b83", signedTx.r)
        assertEquals("0x4466ed2ebb95a22025c110f37b647583b68e229b33b939c625434c6707e09466", signedTx.s)
        assertEquals("0x3063", signedTx.v)
        assertEquals(
            "0xf905088080831c78f69478c310309a973afdcbb88169a16941790137fdbe80b904a498cc6e9800000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000120000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000003e0000000000000000000000000000000000000000000000000000000000000042000000000000000000000000000000000000000000000000000000000000000396469643a656273693a32326d6b6b75667a38696771687136727362337a6c776734326766737770737637656a6e6e79616376796677316a777a00000000000000000000000000000000000000000000000000000000000000000000000000002069270dc0d586a06eb992d0ef5e36309b9033a6428234d9999b8747e2b96580e600000000000000000000000000000000000000000000000000000000000002507b2240636f6e74657874223a5b2268747470733a2f2f77332e6f72672f6e732f6469642f7631225d2c226964223a226469643a656273693a32326d4b4b75667a38694751687136727342335a4c576734326766535770737637454a6e4e79614376794677314a775a222c22766572696669636174696f6e4d6574686f64223a5b7b226964223a226469643a656273693a32326d4b4b75667a38694751687136727342335a4c576734326766535770737637454a6e4e79614376794677314a775a236b65792d31222c2274797065223a22536563703235366b31566572696669636174696f6e4b657932303138222c22636f6e74726f6c6c6572223a226469643a656273693a32326d4b4b75667a38694751687136727342335a4c576734326766535770737637454a6e4e79614376794677314a775a222c227075626c69634b657950656d223a222d2d2d2d2d424547494e205055424c4943204b45592d2d2d2d2d5c6e4d465977454159484b6f5a497a6a3043415159464b34454541416f4451674145313343594c6939513851736b5a57597034744b413576706658744c4d715a55515c6e6f63554c7471674b365339694479717a784531646b4b5a447a50784c384c4547654b5965346739396457614870775564513535584b773d3d5c6e2d2d2d2d2d454e44205055424c4943204b45592d2d2d2d2d227d5d2c2261757468656e7469636174696f6e223a5b226469643a656273693a32326d4b4b75667a38694751687136727342335a4c576734326766535770737637454a6e4e79614376794677314a775a236b65792d31225d7d00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000f7b2264617461223a2274657374227d0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004b7b226d657461223a2264323964353631376562303136623733306636353831643835306231313462396163616534316639313334643164373633633338306664376637616432626662227d000000000000000000000000000000000000000000823063a033ba924eaca0b6f3b0e21de371c15d0f2851432a536432bc2d3160ad17d78b83a04466ed2ebb95a22025c110f37b647583b68e229b33b939c625434c6707e09466",
            signedTx.signedRawTransaction
        )
    }

    private fun getKeyPair(): ECKeyPair =
        ECKeyPair.create(
            buildKey(
                "2552e2fbdc65464bba0348ea6d528ec1",
                KeyAlgorithm.ECDSA_Secp256k1.name,
                "SUN",
                "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEnCOwfFKyIsS4Tu/BrEUac9XskbPVHY+gyvROBtd/mAFA3XJ8zhmLyLhVCZb1x5r+cE/ukHB9xk2tUrx8Gc2RQA==",
                "MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgQlg6QdheWjyyF1OSE8qjYkBQqS3NTji0t5RY5WFXd4+gBwYFK4EEAAqhRANCAAScI7B8UrIixLhO78GsRRpz1eyRs9Udj6DK9E4G13+YAUDdcnzOGYvIuFUJlvXHmv5wT+6QcH3GTa1SvHwZzZFA"
            ).keyPair
        )

    private fun getUnsignedTx(): UnsignedTransaction =
        UnsignedTransaction(
            "0x559345a5a1cfb933cf74f8f50816034a73eb5b8e",
            "0x78c310309A973AFDCbb88169A16941790137fDBe",
            "0x98cc6e9800000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000120000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000003e0000000000000000000000000000000000000000000000000000000000000042000000000000000000000000000000000000000000000000000000000000000396469643a656273693a32326d6b6b75667a38696771687136727362337a6c776734326766737770737637656a6e6e79616376796677316a777a00000000000000000000000000000000000000000000000000000000000000000000000000002069270dc0d586a06eb992d0ef5e36309b9033a6428234d9999b8747e2b96580e600000000000000000000000000000000000000000000000000000000000002507b2240636f6e74657874223a5b2268747470733a2f2f77332e6f72672f6e732f6469642f7631225d2c226964223a226469643a656273693a32326d4b4b75667a38694751687136727342335a4c576734326766535770737637454a6e4e79614376794677314a775a222c22766572696669636174696f6e4d6574686f64223a5b7b226964223a226469643a656273693a32326d4b4b75667a38694751687136727342335a4c576734326766535770737637454a6e4e79614376794677314a775a236b65792d31222c2274797065223a22536563703235366b31566572696669636174696f6e4b657932303138222c22636f6e74726f6c6c6572223a226469643a656273693a32326d4b4b75667a38694751687136727342335a4c576734326766535770737637454a6e4e79614376794677314a775a222c227075626c69634b657950656d223a222d2d2d2d2d424547494e205055424c4943204b45592d2d2d2d2d5c6e4d465977454159484b6f5a497a6a3043415159464b34454541416f4451674145313343594c6939513851736b5a57597034744b413576706658744c4d715a55515c6e6f63554c7471674b365339694479717a784531646b4b5a447a50784c384c4547654b5965346739396457614870775564513535584b773d3d5c6e2d2d2d2d2d454e44205055424c4943204b45592d2d2d2d2d227d5d2c2261757468656e7469636174696f6e223a5b226469643a656273693a32326d4b4b75667a38694751687136727342335a4c576734326766535770737637454a6e4e79614376794677314a775a236b65792d31225d7d00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000f7b2264617461223a2274657374227d0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004b7b226d657461223a2264323964353631376562303136623733306636353831643835306231313462396163616534316639313334643164373633633338306664376637616432626662227d000000000000000000000000000000000000000000",
            "0x00",
            "0x1820",
            "0x1c78f6",
            "0x0",
            "0x0"
        )
}