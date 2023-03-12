package id.walt.services.ecosystems.cheqd.models.job.didstates.finished

import kotlinx.serialization.Serializable

@Serializable
data class DidDocument(
    val authentication: List<String>,
    val controller: List<String>,
    val id: String,
    val verificationMethod: List<VerificationMethod>
)
