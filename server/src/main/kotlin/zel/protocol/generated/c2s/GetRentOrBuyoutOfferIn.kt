package zel.protocol.generated

data class GetRentOrBuyoutOfferIn(
    val isWallItem: Boolean,
    val furniTypeName: String,
    val buyout: Boolean,
)

object GetRentOrBuyoutOfferInCodec : IncomingCodec<GetRentOrBuyoutOfferIn> {
    override val name = "GetRentOrBuyoutOfferIn"

    override fun read(r: PacketReader): GetRentOrBuyoutOfferIn = readGetRentOrBuyoutOfferIn(r)

    override fun write(w: PacketWriter, m: GetRentOrBuyoutOfferIn) = writeGetRentOrBuyoutOfferIn(w, m)

    private fun readGetRentOrBuyoutOfferIn(r: PacketReader): GetRentOrBuyoutOfferIn {
        val vIsWallItem = r.boolean()
        val vFurniTypeName = r.string()
        val vBuyout = r.boolean()
        return GetRentOrBuyoutOfferIn(vIsWallItem, vFurniTypeName, vBuyout)
    }

    private fun writeGetRentOrBuyoutOfferIn(w: PacketWriter, m: GetRentOrBuyoutOfferIn) {
        w.boolean(m.isWallItem)
        w.string(m.furniTypeName)
        w.boolean(m.buyout)
    }
}
