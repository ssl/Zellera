package zel.protocol.generated

data class GetIsOfferGiftableIn(
    val offerId: Int,
)

object GetIsOfferGiftableInCodec : IncomingCodec<GetIsOfferGiftableIn> {
    override val name = "GetIsOfferGiftableIn"

    override fun read(r: PacketReader): GetIsOfferGiftableIn = readGetIsOfferGiftableIn(r)

    override fun write(w: PacketWriter, m: GetIsOfferGiftableIn) = writeGetIsOfferGiftableIn(w, m)

    private fun readGetIsOfferGiftableIn(r: PacketReader): GetIsOfferGiftableIn {
        val vOfferId = r.int()
        return GetIsOfferGiftableIn(vOfferId)
    }

    private fun writeGetIsOfferGiftableIn(w: PacketWriter, m: GetIsOfferGiftableIn) {
        w.int(m.offerId)
    }
}
