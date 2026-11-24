package zel.protocol.generated

data object GetMarketplaceCanMakeOfferIn

object GetMarketplaceCanMakeOfferInCodec : IncomingCodec<GetMarketplaceCanMakeOfferIn> {
    override val name = "GetMarketplaceCanMakeOfferIn"

    override fun read(r: PacketReader): GetMarketplaceCanMakeOfferIn = GetMarketplaceCanMakeOfferIn

    override fun write(w: PacketWriter, m: GetMarketplaceCanMakeOfferIn) = writeGetMarketplaceCanMakeOfferIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetMarketplaceCanMakeOfferIn(w: PacketWriter, m: GetMarketplaceCanMakeOfferIn) {
    }
}
