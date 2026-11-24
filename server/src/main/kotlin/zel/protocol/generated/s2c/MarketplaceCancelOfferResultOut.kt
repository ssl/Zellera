package zel.protocol.generated

data class MarketplaceCancelOfferResultOut(
    val int1: Int,
    val boolean1: Boolean,
)

object MarketplaceCancelOfferResultOutCodec : OutgoingCodec<MarketplaceCancelOfferResultOut> {
    override val name = "MarketplaceCancelOfferResultOut"

    override fun read(r: PacketReader): MarketplaceCancelOfferResultOut = readMarketplaceCancelOfferResultOut(r)

    override fun write(w: PacketWriter, m: MarketplaceCancelOfferResultOut) = writeMarketplaceCancelOfferResultOut(w, m)

    private fun readMarketplaceCancelOfferResultOut(r: PacketReader): MarketplaceCancelOfferResultOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return MarketplaceCancelOfferResultOut(vInt1, vBoolean1)
    }

    private fun writeMarketplaceCancelOfferResultOut(w: PacketWriter, m: MarketplaceCancelOfferResultOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
    }
}
