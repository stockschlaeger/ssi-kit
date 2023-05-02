package id.walt.signatory.revocation.statuslist2021

import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.W3CCredentialSubject
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import id.walt.credentials.w3c.templates.VcTemplateService
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.context.ContextManager
import id.walt.services.did.DidService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory

open class StatusListCredentialStorageService : WaltIdService() {
    override val implementation get() = serviceImplementation<StatusListCredentialStorageService>()

    open fun fetch(id: String): VerifiableCredential? = implementation.fetch(id)
    open fun store(id: String, purpose: String, bitString: String): Unit = implementation.store(id, purpose, bitString)

    companion object : ServiceProvider {
        override fun getService() = object : StatusListCredentialStorageService() {}
        override fun defaultImplementation() = WaltIdStatusListCredentialStorageService()
    }
}


class WaltIdStatusListCredentialStorageService : StatusListCredentialStorageService() {
    private val templatePath = "StatusList2021Credential"
    private val credentialsGroup = "status-credentials"
    private val signatoryService = Signatory.getService()
    private val vcStoreService = ContextManager.vcStore
    private val templateService = VcTemplateService.getService()
    private val issuerDid = DidService.create(DidMethod.key)// TODO: fix it

    override fun fetch(id: String): VerifiableCredential? =
        vcStoreService.getCredential(id.substringAfterLast("/"), credentialsGroup)
    override fun store(id: String, purpose: String, bitString: String): Unit = let {
        fetch(id)?.let { vc ->
            // update vc
            W3CCredentialSubject(
                vc.id, mapOf(
                    "type" to vc.credentialSubject!!.properties["type"] as String,
                    "statusPurpose" to vc.credentialSubject!!.properties["statusPurpose"] as String,
                    "encodedList" to bitString
                )
            )
            // new vc
        } ?: W3CCredentialSubject(
            id, mapOf(
                "type" to "StatusList2021Credential",
                "statusPurpose" to purpose,
                "encodedList" to bitString
            )
        )
    }.let {
        W3CCredentialBuilder.fromPartial(templateService.getTemplate(templatePath).template!!).apply {
            setId(it.id ?: id)
            buildSubject {
                setFromJson(it.toJson())
            }
        }
    }.run {
        val credential = signatoryService.issue(
            credentialBuilder = this, config = ProofConfig(
                credentialId = id,
                issuerDid = issuerDid,
                subjectDid = issuerDid,
                proofType = ProofType.LD_PROOF,
            )
        ).toVerifiableCredential()
        vcStoreService.storeCredential(credential.id!!.substringAfterLast("/"), credential, credentialsGroup)
    }
}
