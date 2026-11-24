package zel.protocol.generated

data class MarketplaceCancelAllOffersResultOut(
    val list1: List<Int>,
    val boolean1: Boolean,
)

object MarketplaceCancelAllOffersResultOutCodec : OutgoingCodec<MarketplaceCancelAllOffersResultOut> {
    override val name = "MarketplaceCancelAllOffersResultOut"

    override fun read(r: PacketReader): MarketplaceCancelAllOffersResultOut = readMarketplaceCancelAllOffersResultOut(r)

    override fun write(w: PacketWriter, m: MarketplaceCancelAllOffersResultOut) = writeMarketplaceCancelAllOffersResultOut(w, m)

    private fun readMarketplaceCancelAllOffersResultOut(r: PacketReader): MarketplaceCancelAllOffersResultOut {
        val vList1 = List(r.countInt(4)) { r.int() }
        val vBoolean1 = r.boolean()
        return MarketplaceCancelAllOffersResultOut(vList1, vBoolean1)
    }

    private fun writeMarketplaceCancelAllOffersResultOut(w: PacketWriter, m: MarketplaceCancelAllOffersResultOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.boolean(m.boolean1)
    }
}
