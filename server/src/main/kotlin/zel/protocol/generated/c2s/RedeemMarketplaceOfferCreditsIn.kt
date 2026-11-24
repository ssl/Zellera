package zel.protocol.generated

data object RedeemMarketplaceOfferCreditsIn

object RedeemMarketplaceOfferCreditsInCodec : IncomingCodec<RedeemMarketplaceOfferCreditsIn> {
    override val name = "RedeemMarketplaceOfferCreditsIn"

    override fun read(r: PacketReader): RedeemMarketplaceOfferCreditsIn = RedeemMarketplaceOfferCreditsIn

    override fun write(w: PacketWriter, m: RedeemMarketplaceOfferCreditsIn) = writeRedeemMarketplaceOfferCreditsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeRedeemMarketplaceOfferCreditsIn(w: PacketWriter, m: RedeemMarketplaceOfferCreditsIn) {
    }
}
