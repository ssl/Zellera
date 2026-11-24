package zel.protocol.generated

data object BuyMarketplaceTokensIn

object BuyMarketplaceTokensInCodec : IncomingCodec<BuyMarketplaceTokensIn> {
    override val name = "BuyMarketplaceTokensIn"

    override fun read(r: PacketReader): BuyMarketplaceTokensIn = BuyMarketplaceTokensIn

    override fun write(w: PacketWriter, m: BuyMarketplaceTokensIn) = writeBuyMarketplaceTokensIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeBuyMarketplaceTokensIn(w: PacketWriter, m: BuyMarketplaceTokensIn) {
    }
}
