package zel.protocol.generated

data class MarketPlaceOwnOffersOut(
    val int1: Int,
    val rest: ByteArray,
)

object MarketPlaceOwnOffersOutCodec : OutgoingCodec<MarketPlaceOwnOffersOut> {
    override val name = "MarketPlaceOwnOffersOut"

    override fun read(r: PacketReader): MarketPlaceOwnOffersOut = readMarketPlaceOwnOffersOut(r)

    override fun write(w: PacketWriter, m: MarketPlaceOwnOffersOut) = writeMarketPlaceOwnOffersOut(w, m)

    private fun readMarketPlaceOwnOffersOut(r: PacketReader): MarketPlaceOwnOffersOut {
        val vInt1 = r.int()
        val vRest = r.rest()
        return MarketPlaceOwnOffersOut(vInt1, vRest)
    }

    private fun writeMarketPlaceOwnOffersOut(w: PacketWriter, m: MarketPlaceOwnOffersOut) {
        w.int(m.int1)
        w.bytes(m.rest)
    }
}
