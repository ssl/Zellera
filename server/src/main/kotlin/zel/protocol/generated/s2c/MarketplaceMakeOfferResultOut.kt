package zel.protocol.generated

data class MarketplaceMakeOfferResultOut(
    val int1: Int,
)

object MarketplaceMakeOfferResultOutCodec : OutgoingCodec<MarketplaceMakeOfferResultOut> {
    override val name = "MarketplaceMakeOfferResultOut"

    override fun read(r: PacketReader): MarketplaceMakeOfferResultOut = readMarketplaceMakeOfferResultOut(r)

    override fun write(w: PacketWriter, m: MarketplaceMakeOfferResultOut) = writeMarketplaceMakeOfferResultOut(w, m)

    private fun readMarketplaceMakeOfferResultOut(r: PacketReader): MarketplaceMakeOfferResultOut {
        val vInt1 = r.int()
        return MarketplaceMakeOfferResultOut(vInt1)
    }

    private fun writeMarketplaceMakeOfferResultOut(w: PacketWriter, m: MarketplaceMakeOfferResultOut) {
        w.int(m.int1)
    }
}
