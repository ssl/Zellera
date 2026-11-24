package zel.protocol.generated

data object CancelAllMarketplaceOffersIn

object CancelAllMarketplaceOffersInCodec : IncomingCodec<CancelAllMarketplaceOffersIn> {
    override val name = "CancelAllMarketplaceOffersIn"

    override fun read(r: PacketReader): CancelAllMarketplaceOffersIn = CancelAllMarketplaceOffersIn

    override fun write(w: PacketWriter, m: CancelAllMarketplaceOffersIn) = writeCancelAllMarketplaceOffersIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeCancelAllMarketplaceOffersIn(w: PacketWriter, m: CancelAllMarketplaceOffersIn) {
    }
}
