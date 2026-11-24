package zel.protocol.generated

data class MarketplaceCanMakeOfferResultOut(
    val int1: Int,
    val int2: Int,
)

object MarketplaceCanMakeOfferResultOutCodec : OutgoingCodec<MarketplaceCanMakeOfferResultOut> {
    override val name = "MarketplaceCanMakeOfferResultOut"

    override fun read(r: PacketReader): MarketplaceCanMakeOfferResultOut = readMarketplaceCanMakeOfferResultOut(r)

    override fun write(w: PacketWriter, m: MarketplaceCanMakeOfferResultOut) = writeMarketplaceCanMakeOfferResultOut(w, m)

    private fun readMarketplaceCanMakeOfferResultOut(r: PacketReader): MarketplaceCanMakeOfferResultOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return MarketplaceCanMakeOfferResultOut(vInt1, vInt2)
    }

    private fun writeMarketplaceCanMakeOfferResultOut(w: PacketWriter, m: MarketplaceCanMakeOfferResultOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
