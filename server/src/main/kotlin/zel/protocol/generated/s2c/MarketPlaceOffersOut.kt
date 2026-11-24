package zel.protocol.generated

data class MarketPlaceOffersOut(
    val rest: ByteArray,
)

object MarketPlaceOffersOutCodec : OutgoingCodec<MarketPlaceOffersOut> {
    override val name = "MarketPlaceOffersOut"

    override fun read(r: PacketReader): MarketPlaceOffersOut = readMarketPlaceOffersOut(r)

    override fun write(w: PacketWriter, m: MarketPlaceOffersOut) = writeMarketPlaceOffersOut(w, m)

    private fun readMarketPlaceOffersOut(r: PacketReader): MarketPlaceOffersOut {
        val vRest = r.rest()
        return MarketPlaceOffersOut(vRest)
    }

    private fun writeMarketPlaceOffersOut(w: PacketWriter, m: MarketPlaceOffersOut) {
        w.bytes(m.rest)
    }
}
