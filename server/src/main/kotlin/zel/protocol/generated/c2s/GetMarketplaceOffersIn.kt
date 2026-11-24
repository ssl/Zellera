package zel.protocol.generated

data class GetMarketplaceOffersIn(
    val min: Int,
    val max: Int,
    val query: String,
    val type: Int,
    val combineUniques: Boolean?,
)

object GetMarketplaceOffersInCodec : IncomingCodec<GetMarketplaceOffersIn> {
    override val name = "GetMarketplaceOffersIn"

    override fun read(r: PacketReader): GetMarketplaceOffersIn = readGetMarketplaceOffersIn(r)

    override fun write(w: PacketWriter, m: GetMarketplaceOffersIn) = writeGetMarketplaceOffersIn(w, m)

    private fun readGetMarketplaceOffersIn(r: PacketReader): GetMarketplaceOffersIn {
        val vMin = r.int()
        val vMax = r.int()
        val vQuery = r.string()
        val vType = r.int()
        val vCombineUniques = if (r.remaining > 0) r.boolean() else null
        return GetMarketplaceOffersIn(vMin, vMax, vQuery, vType, vCombineUniques)
    }

    private fun writeGetMarketplaceOffersIn(w: PacketWriter, m: GetMarketplaceOffersIn) {
        w.int(m.min)
        w.int(m.max)
        w.string(m.query)
        w.int(m.type)
        m.combineUniques?.let { w.boolean(it) }
    }
}
