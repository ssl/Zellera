package zel.protocol.generated

data class MarketplaceBuyOfferResultOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
)

object MarketplaceBuyOfferResultOutCodec : OutgoingCodec<MarketplaceBuyOfferResultOut> {
    override val name = "MarketplaceBuyOfferResultOut"

    override fun read(r: PacketReader): MarketplaceBuyOfferResultOut = readMarketplaceBuyOfferResultOut(r)

    override fun write(w: PacketWriter, m: MarketplaceBuyOfferResultOut) = writeMarketplaceBuyOfferResultOut(w, m)

    private fun readMarketplaceBuyOfferResultOut(r: PacketReader): MarketplaceBuyOfferResultOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        return MarketplaceBuyOfferResultOut(vInt1, vInt2, vInt3, vInt4)
    }

    private fun writeMarketplaceBuyOfferResultOut(w: PacketWriter, m: MarketplaceBuyOfferResultOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
    }
}
