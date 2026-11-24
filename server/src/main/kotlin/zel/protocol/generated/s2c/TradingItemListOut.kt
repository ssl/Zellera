package zel.protocol.generated

data class TradingItemListOut(
    val int1: Int,
    val rest: ByteArray,
)

object TradingItemListOutCodec : OutgoingCodec<TradingItemListOut> {
    override val name = "TradingItemListOut"

    override fun read(r: PacketReader): TradingItemListOut = readTradingItemListOut(r)

    override fun write(w: PacketWriter, m: TradingItemListOut) = writeTradingItemListOut(w, m)

    private fun readTradingItemListOut(r: PacketReader): TradingItemListOut {
        val vInt1 = r.int()
        val vRest = r.rest()
        return TradingItemListOut(vInt1, vRest)
    }

    private fun writeTradingItemListOut(w: PacketWriter, m: TradingItemListOut) {
        w.int(m.int1)
        w.bytes(m.rest)
    }
}
