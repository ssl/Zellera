package zel.protocol.generated

data class BuyMarketplaceOfferIn(
    val offerId: Int,
)

object BuyMarketplaceOfferInCodec : IncomingCodec<BuyMarketplaceOfferIn> {
    override val name = "BuyMarketplaceOfferIn"

    override fun read(r: PacketReader): BuyMarketplaceOfferIn = readBuyMarketplaceOfferIn(r)

    override fun write(w: PacketWriter, m: BuyMarketplaceOfferIn) = writeBuyMarketplaceOfferIn(w, m)

    private fun readBuyMarketplaceOfferIn(r: PacketReader): BuyMarketplaceOfferIn {
        val vOfferId = r.int()
        return BuyMarketplaceOfferIn(vOfferId)
    }

    private fun writeBuyMarketplaceOfferIn(w: PacketWriter, m: BuyMarketplaceOfferIn) {
        w.int(m.offerId)
    }
}
