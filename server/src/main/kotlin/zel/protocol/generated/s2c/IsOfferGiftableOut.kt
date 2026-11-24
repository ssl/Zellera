package zel.protocol.generated

data class IsOfferGiftableOut(
    val int1: Int,
    val boolean1: Boolean,
)

object IsOfferGiftableOutCodec : OutgoingCodec<IsOfferGiftableOut> {
    override val name = "IsOfferGiftableOut"

    override fun read(r: PacketReader): IsOfferGiftableOut = readIsOfferGiftableOut(r)

    override fun write(w: PacketWriter, m: IsOfferGiftableOut) = writeIsOfferGiftableOut(w, m)

    private fun readIsOfferGiftableOut(r: PacketReader): IsOfferGiftableOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return IsOfferGiftableOut(vInt1, vBoolean1)
    }

    private fun writeIsOfferGiftableOut(w: PacketWriter, m: IsOfferGiftableOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
    }
}
