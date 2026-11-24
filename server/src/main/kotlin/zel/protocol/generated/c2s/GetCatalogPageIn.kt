package zel.protocol.generated

data class GetCatalogPageIn(
    val catalogPageId: Int,
    val offerId: Int,
    val mode: String,
)

object GetCatalogPageInCodec : IncomingCodec<GetCatalogPageIn> {
    override val name = "GetCatalogPageIn"

    override fun read(r: PacketReader): GetCatalogPageIn = readGetCatalogPageIn(r)

    override fun write(w: PacketWriter, m: GetCatalogPageIn) = writeGetCatalogPageIn(w, m)

    private fun readGetCatalogPageIn(r: PacketReader): GetCatalogPageIn {
        val vCatalogPageId = r.int()
        val vOfferId = r.int()
        val vMode = r.string()
        return GetCatalogPageIn(vCatalogPageId, vOfferId, vMode)
    }

    private fun writeGetCatalogPageIn(w: PacketWriter, m: GetCatalogPageIn) {
        w.int(m.catalogPageId)
        w.int(m.offerId)
        w.string(m.mode)
    }
}
