package zel.protocol.generated

data class GetProductOfferIn(
    val offerId: Int,
)

object GetProductOfferInCodec : IncomingCodec<GetProductOfferIn> {
    override val name = "GetProductOfferIn"

    override fun read(r: PacketReader): GetProductOfferIn = readGetProductOfferIn(r)

    override fun write(w: PacketWriter, m: GetProductOfferIn) = writeGetProductOfferIn(w, m)

    private fun readGetProductOfferIn(r: PacketReader): GetProductOfferIn {
        val vOfferId = r.int()
        return GetProductOfferIn(vOfferId)
    }

    private fun writeGetProductOfferIn(w: PacketWriter, m: GetProductOfferIn) {
        w.int(m.offerId)
    }
}
