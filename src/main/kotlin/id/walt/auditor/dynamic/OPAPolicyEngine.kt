package id.walt.auditor.dynamic

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import id.walt.auditor.VerificationPolicyResult
import id.walt.common.resolveContentToFile
import id.walt.credentials.w3c.JsonConverter
import kotlinx.serialization.json.buildJsonObject
import mu.KotlinLogging
import java.io.File

object OPAPolicyEngine : PolicyEngine {
    private val log = KotlinLogging.logger {}

    const val TEMP_PREFIX = "_TEMP_"

    override fun validate(input: PolicyEngineInput, policy: String, query: String): VerificationPolicyResult {
        try {
            ProcessBuilder("opa").start()
        } catch (e: Exception) {
            return VerificationPolicyResult.failure(IllegalStateException("Executable for OPA policy engine not installed. See https://www.openpolicyagent.org/docs/#running-opa"))
        }
        val regoFile = resolveContentToFile(policy, tempPrefix = TEMP_PREFIX, tempPostfix = ".rego")
        try {
            val p = ProcessBuilder(
                "opa",
                "eval",
                "-d",
                regoFile.absolutePath,
                "-I",
                "-f",
                "values",
                query
            ).start()
            p.outputStream.writer().use { it.write(input.toJson()) }
            val output = p.inputStream.reader().use { it.readText() }
            p.waitFor()
            log.debug("rego eval output: {}", output)
            return VerificationPolicyResult(Klaxon().parseArray<Boolean>(output)?.all { it } ?: false)
        } finally {
            if (regoFile.exists() && regoFile.name.startsWith(TEMP_PREFIX)) {
                regoFile.delete()
            }
        }
    }

    override val type: PolicyEngineType = PolicyEngineType.OPA
}
