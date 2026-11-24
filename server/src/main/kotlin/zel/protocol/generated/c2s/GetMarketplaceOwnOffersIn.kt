package zel.protocol.generated

data class GetMarketplaceOwnOffersIn(
    val int1: Int,
)

object GetMarketplaceOwnOffersInCodec : IncomingCodec<GetMarketplaceOwnOffersIn> {
    override val name = "GetMarketplaceOwnOffersIn"

    override fun read(r: PacketReader): GetMarketplaceOwnOffersIn = readGetMarketplaceOwnOffersIn(r)

    override fun write(w: PacketWriter, m: GetMarketplaceOwnOffersIn) = writeGetMarketplaceOwnOffersIn(w, m)

    private fun readGetMarketplaceOwnOffersIn(r: PacketReader): GetMarketplaceOwnOffersIn {
        val vInt1 = r.int()
        return GetMarketplaceOwnOffersIn(vInt1)
    }

    private fun writeGetMarketplaceOwnOffersIn(w: PacketWriter, m: GetMarketplaceOwnOffersIn) {
        w.int(m.int1)
    }
}
