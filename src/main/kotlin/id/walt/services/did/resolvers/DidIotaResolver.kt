package id.walt.services.did.resolvers

import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.model.did.DidIota
import id.walt.services.ecosystems.iota.IotaWrapper

class DidIotaResolver(private val iotaWrapper: IotaWrapper) : DidResolverBase<DidIota>() {
    override fun resolve(didUrl: DidUrl): Did = iotaWrapper.resolve_did(didUrl.did).takeIf { it.address() != 0L }?.let {
        val doc = it.getString(0)
        iotaWrapper.free_str(it)
        Did.decode(doc)
    } ?: throw Exception("Could not resolve $didUrl")
}