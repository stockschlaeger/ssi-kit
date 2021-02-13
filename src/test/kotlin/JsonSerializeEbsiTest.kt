import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.*
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals


class JsonSerializeEbsiTest {

    val format = Json { prettyPrint = true }

    private fun validateVC(fileName: String) {
        val expected = File("src/test/resources/ebsi/${fileName}").readText()
        // println(expected)
        val obj = Json.decodeFromString<VerifiableCredential>(expected)
        // println(obj)
        val encoded = Json.encodeToString(obj)
        // println(encoded)
        assertEquals(expected.replace("\\s".toRegex(), ""), Json.encodeToString(obj))
    }

    @Test
    fun ebsiDidTest() {
        val expected = File("src/test/resources/dids/did-ebsi.json").readText()
        println(expected)
        val obj = Json.decodeFromString<DidEbsi>(expected)
        println(obj)
        val encoded = Json.encodeToString(obj)
        println(encoded)
        assertEquals(expected.replace("\\s".toRegex(), ""), Json.encodeToString(obj))
    }

    @Test
    fun verifiableAuthorizationTest() {
        validateVC("verifiable-authorization.json")
    }

    @Test
    fun verifiableAttestationTest() {
        validateVC("verifiable-attestation.json")
    }

    @Test
    fun credentialStatusListTest() {
        val expected = File("src/test/resources/ebsi/verifiable-credential-status.json").readText()
        println(expected)
        val obj = Json.decodeFromString<List<CredentialStatusListEntry>>(expected)
        println(obj)
        assertEquals(expected.replace("\\s".toRegex(), ""), Json.encodeToString(obj))
    }

    @Test
    fun trustedIssuerRegistryObjTest() {

        var did = listOf<String>("did:ebsi:00003333", "did:ebsi:00005555")
        var organizationInfo =
            OrganizationInfo("https://essif.europa.eu/tsr/53", "Great Company", "Great Company Street 1, Brussels, Belgium", "BE05555555XX", "https://great.company.be")
        val proof = Proof("EidasSeal2019", LocalDateTime.now().withNano(0), "assertionMethod", VerificationMethod("EidasCertificate2019", "1088321447"), "BD21J4fdlnBvBA+y6D...fnC8Y=")
        val serviceEndpoints = listOf<ServiceEndpoint>(ServiceEndpoint("did:example:123456789abcdefghi#agent", "AgentService", "https://agent.example.com/8377464"))
        val eidasCertificate = EidasCertificate("123456", "123456", "blob")
        var issuer = Issuer("Brand Name", did, eidasCertificate, serviceEndpoints, organizationInfo)
        var accreditationCredentials = listOf<VerifiableCredential>(
            VerifiableCredential(
                listOf(
                    "https://www.w3.org/2018/credentials/v1",
                    "https://essif.europa.eu/schemas/vc/2020/v1"
                ),
                "https://essif.europa.eu/tsr/53",
                listOf<String>("VerifiableCredential", "VerifiableAttestation"),
                "did:ebsi:000098765",
                LocalDateTime.now().withNano(0),
                CredentialSubject("did:ebsi:00001235", null, listOf("claim1", "claim2")),
                CredentialStatus("https://essif.europa.eu/status/45", "CredentialsStatusList2020"),
                CredentialSchema("https://essif.europa.eu/tsr/education/CSR1224.json", "JsonSchemaValidator2018"),
                proof
            )
        )

        var tir = TrustedIssuerRegistry(issuer, accreditationCredentials)

        val string = format.encodeToString(tir)
        println(string)

        val obj = Json.decodeFromString<TrustedIssuerRegistry>(string)
        println(obj)

        assertEquals(tir, obj)
    }

    @Test
    fun trustedIssuerRegistryFileTest() {
        val expected = File("src/test/resources/ebsi/trusted-issuer-registry.json").readText()
        val obj = Json.decodeFromString<TrustedIssuerRegistry>(expected)
        println(obj)
        val string = format.encodeToString(obj)
        println(string)
        assertEquals(expected.replace("\\s".toRegex(), ""), string.replace("\\s".toRegex(), ""))
    }

    @Test
    fun dateTest() {

        val inDateTime = ZonedDateTime.of(LocalDateTime.now(), ZoneOffset.UTC)

        val inDateEpochSeconds = Instant.ofEpochSecond(inDateTime.toEpochSecond())

        val dateStr = DateTimeFormatter.ISO_INSTANT.format(inDateEpochSeconds)

        println("STRING:  " + dateStr) // 2021-02-11T15:38:00Z

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val outDateTime = LocalDateTime.parse(dateStr, formatter)

        println("DATE TIME:  " + outDateTime) // 2021-02-11T15:41:01

    }
}
