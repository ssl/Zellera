package zel.protocol.generated

data class CancelMarketplaceOfferIn(
    val offerId: Int,
)

object CancelMarketplaceOfferInCodec : IncomingCodec<CancelMarketplaceOfferIn> {
    override val name = "CancelMarketplaceOfferIn"

    override fun read(r: PacketReader): CancelMarketplaceOfferIn = readCancelMarketplaceOfferIn(r)

    override fun write(w: PacketWriter, m: CancelMarketplaceOfferIn) = writeCancelMarketplaceOfferIn(w, m)

    private fun readCancelMarketplaceOfferIn(r: PacketReader): CancelMarketplaceOfferIn {
        val vOfferId = r.int()
        return CancelMarketplaceOfferIn(vOfferId)
    }

    private fun writeCancelMarketplaceOfferIn(w: PacketWriter, m: CancelMarketplaceOfferIn) {
        w.int(m.offerId)
    }
}
