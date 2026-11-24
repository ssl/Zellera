package zel.protocol.generated

data class PurchaseTargetedOfferIn(
    val offerId: Int,
    val amount: Int,
)

object PurchaseTargetedOfferInCodec : IncomingCodec<PurchaseTargetedOfferIn> {
    override val name = "PurchaseTargetedOfferIn"

    override fun read(r: PacketReader): PurchaseTargetedOfferIn = readPurchaseTargetedOfferIn(r)

    override fun write(w: PacketWriter, m: PurchaseTargetedOfferIn) = writePurchaseTargetedOfferIn(w, m)

    private fun readPurchaseTargetedOfferIn(r: PacketReader): PurchaseTargetedOfferIn {
        val vOfferId = r.int()
        val vAmount = r.int()
        return PurchaseTargetedOfferIn(vOfferId, vAmount)
    }

    private fun writePurchaseTargetedOfferIn(w: PacketWriter, m: PurchaseTargetedOfferIn) {
        w.int(m.offerId)
        w.int(m.amount)
    }
}
