package id.walt.services.essif

import id.walt.servicematrix.ServiceMatrix
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.crypto.buildKey
import id.walt.services.essif.didebsi.DidEbsiService
import id.walt.services.essif.didebsi.UnsignedTransaction
import id.walt.services.keystore.KeyStoreService
import java.nio.file.Files
import java.nio.file.Path

class DidEbsiServiceTest : AnnotationSpec() {

    companion object {
        private val KEY_ID = KeyId("DidEbsiServiceTest_key")
        private const val DID = "did:ebsi:23R3YwWEc7J1chejmwjh5JDaRjqvvf6ogHnxJNHUvaep4f98"
        private val DID_FILENAME = DID.replace(":", "-") + ".json"
    }

    init {
        println("Running ServiceMatrix")
        ServiceMatrix("service-matrix.properties")
        println("Done running the ServiceMatrix")
    }

    private val didEbsiService = DidEbsiService.getService()
    private val keyStore = KeyStoreService.getService()
    private val key = buildKey(
        KEY_ID.id,
        KeyAlgorithm.ECDSA_Secp256k1.name,
        "SUN",
        "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEnCOwfFKyIsS4Tu/BrEUac9XskbPVHY+gyvROBtd/mAFA3XJ8zhmLyLhVCZb1x5r+cE/ukHB9xk2tUrx8Gc2RQA==",
        "MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgQlg6QdheWjyyF1OSE8qjYkBQqS3NTji0t5RY5WFXd4+gBwYFK4EEAAqhRANCAAScI7B8UrIixLhO78GsRRpz1eyRs9Udj6DK9E4G13+YAUDdcnzOGYvIuFUJlvXHmv5wT+6QcH3GTa1SvHwZzZFA"
    )

    @Before
    fun setup() {
        Files.copy(
            Path.of("src", "test", "resources", "ebsi", DID_FILENAME),
            Path.of("data", "did", "created", DID_FILENAME)
        )
        keyStore.store(key)
        keyStore.addAlias(KEY_ID, DID)
        keyStore.addAlias(KEY_ID, "$DID#key-1")
    }

    @After
    fun clean() {
        Files.delete(Path.of("data", "did", "created", DID_FILENAME))
        keyStore.delete(KEY_ID.id)
    }

    @Test
    fun testBuildInsertDocumentParams() {
        println("NEEDING DID EBSI 1")
        val params = didEbsiService.buildInsertDocumentParams(DID)[0]

        params.from shouldBe "0x7bfA7efe33fD22aaE73bE80eC9901755F55065c2"
        params.identifier shouldBe "0x6469643a656273693a323352335977574563374a316368656a6d776a68354a4461526a71767666366f67486e784a4e48557661657034663938"
        params.hashAlgorithmId shouldBe 1
        // params.hashValue shouldBe "0x1761d3a6bc3eca2d4460e04ec65e5b4fdd490114ba690f0832e0d245ac7a5612" // TODO FIXME
        // params.didVersionInfo shouldBe "0x7b2240636f6e74657874223a5b2268747470733a2f2f77332e6f72672f6e732f6469642f7631225d2c226964223a226469643a656273693a323352335977574563374a316368656a6d776a68354a4461526a71767666366f67486e784a4e48557661657034663938222c22766572696669636174696f6e4d6574686f64223a5b7b226964223a226469643a656273693a323352335977574563374a316368656a6d776a68354a4461526a71767666366f67486e784a4e48557661657034663938236b65792d31222c2274797065223a22536563703235366b31566572696669636174696f6e4b657932303138222c22636f6e74726f6c6c6572223a226469643a656273693a323352335977574563374a316368656a6d776a68354a4461526a71767666366f67486e784a4e48557661657034663938222c227075626c69634b65794a776b223a7b226b6964223a224469644562736953657276696365546573745f6b6579222c226b7479223a224543222c22616c67223a2245533235364b222c22637276223a22736563703235366b31222c22757365223a22736967222c2278223a226e434f7766464b794973533454755f4272455561633958736b62505648592d677976524f4274645f6d4145222c2279223a22514e3179664d345a6938693456516d57396365615f6e42503770427766635a4e72564b3866426e4e6b5541227d7d5d2c2261757468656e7469636174696f6e223a5b226469643a656273693a323352335977574563374a316368656a6d776a68354a4461526a71767666366f67486e784a4e48557661657034663938236b65792d31225d7d"
        params.timestampData shouldBe "0x7b2264617461223a2274657374227d"
        params.didVersionMetadata.length shouldBe 152
    }

    @Test
    fun testSignTransaction() {
        val unsignedTransaction = getUnsignedTx()
        println("NEEDING DID EBSI 2")
        val signedTx = didEbsiService.signTransaction(DID, unsignedTransaction)
        signedTx.r shouldBe "0x33ba924eaca0b6f3b0e21de371c15d0f2851432a536432bc2d3160ad17d78b83"
        signedTx.s shouldBe "0x4466ed2ebb95a22025c110f37b647583b68e229b33b939c625434c6707e09466"
        signedTx.v shouldBe "0x3063"
        signedTx.signedRawTransaction shouldBe "0xf905088080831c78f69478c310309a973afdcbb88169a16941790137fdbe80b904a498cc6e9800000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000120000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000003e0000000000000000000000000000000000000000000000000000000000000042000000000000000000000000000000000000000000000000000000000000000396469643a656273693a32326d6b6b75667a38696771687136727362337a6c776734326766737770737637656a6e6e79616376796677316a777a00000000000000000000000000000000000000000000000000000000000000000000000000002069270dc0d586a06eb992d0ef5e36309b9033a6428234d9999b8747e2b96580e600000000000000000000000000000000000000000000000000000000000002507b2240636f6e74657874223a5b2268747470733a2f2f77332e6f72672f6e732f6469642f7631225d2c226964223a226469643a656273693a32326d4b4b75667a38694751687136727342335a4c576734326766535770737637454a6e4e79614376794677314a775a222c22766572696669636174696f6e4d6574686f64223a5b7b226964223a226469643a656273693a32326d4b4b75667a38694751687136727342335a4c576734326766535770737637454a6e4e79614376794677314a775a236b65792d31222c2274797065223a22536563703235366b31566572696669636174696f6e4b657932303138222c22636f6e74726f6c6c6572223a226469643a656273693a32326d4b4b75667a38694751687136727342335a4c576734326766535770737637454a6e4e79614376794677314a775a222c227075626c69634b657950656d223a222d2d2d2d2d424547494e205055424c4943204b45592d2d2d2d2d5c6e4d465977454159484b6f5a497a6a3043415159464b34454541416f4451674145313343594c6939513851736b5a57597034744b413576706658744c4d715a55515c6e6f63554c7471674b365339694479717a784531646b4b5a447a50784c384c4547654b5965346739396457614870775564513535584b773d3d5c6e2d2d2d2d2d454e44205055424c4943204b45592d2d2d2d2d227d5d2c2261757468656e7469636174696f6e223a5b226469643a656273693a32326d4b4b75667a38694751687136727342335a4c576734326766535770737637454a6e4e79614376794677314a775a236b65792d31225d7d00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000f7b2264617461223a2274657374227d0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004b7b226d657461223a2264323964353631376562303136623733306636353831643835306231313462396163616534316639313334643164373633633338306664376637616432626662227d000000000000000000000000000000000000000000823063a033ba924eaca0b6f3b0e21de371c15d0f2851432a536432bc2d3160ad17d78b83a04466ed2ebb95a22025c110f37b647583b68e229b33b939c625434c6707e09466"
    }

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