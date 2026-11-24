package zel.protocol.generated

data class ShopTargetedOfferViewedIn(
    val int1: Int,
    val int2: Int,
)

object ShopTargetedOfferViewedInCodec : IncomingCodec<ShopTargetedOfferViewedIn> {
    override val name = "ShopTargetedOfferViewedIn"

    override fun read(r: PacketReader): ShopTargetedOfferViewedIn = readShopTargetedOfferViewedIn(r)

    override fun write(w: PacketWriter, m: ShopTargetedOfferViewedIn) = writeShopTargetedOfferViewedIn(w, m)

    private fun readShopTargetedOfferViewedIn(r: PacketReader): ShopTargetedOfferViewedIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return ShopTargetedOfferViewedIn(vInt1, vInt2)
    }

    private fun writeShopTargetedOfferViewedIn(w: PacketWriter, m: ShopTargetedOfferViewedIn) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
