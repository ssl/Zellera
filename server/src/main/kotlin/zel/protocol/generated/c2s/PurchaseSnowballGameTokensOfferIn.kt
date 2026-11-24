package zel.protocol.generated

data class PurchaseSnowballGameTokensOfferIn(
    val int1: Int,
)

object PurchaseSnowballGameTokensOfferInCodec : IncomingCodec<PurchaseSnowballGameTokensOfferIn> {
    override val name = "PurchaseSnowballGameTokensOfferIn"

    override fun read(r: PacketReader): PurchaseSnowballGameTokensOfferIn = readPurchaseSnowballGameTokensOfferIn(r)

    override fun write(w: PacketWriter, m: PurchaseSnowballGameTokensOfferIn) = writePurchaseSnowballGameTokensOfferIn(w, m)

    private fun readPurchaseSnowballGameTokensOfferIn(r: PacketReader): PurchaseSnowballGameTokensOfferIn {
        val vInt1 = r.int()
        return PurchaseSnowballGameTokensOfferIn(vInt1)
    }

    private fun writePurchaseSnowballGameTokensOfferIn(w: PacketWriter, m: PurchaseSnowballGameTokensOfferIn) {
        w.int(m.int1)
    }
}
