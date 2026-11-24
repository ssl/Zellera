package zel.protocol.generated

data class TradingCloseOut(
    val int1: Int,
    val int2: Int,
)

object TradingCloseOutCodec : OutgoingCodec<TradingCloseOut> {
    override val name = "TradingCloseOut"

    override fun read(r: PacketReader): TradingCloseOut = readTradingCloseOut(r)

    override fun write(w: PacketWriter, m: TradingCloseOut) = writeTradingCloseOut(w, m)

    private fun readTradingCloseOut(r: PacketReader): TradingCloseOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return TradingCloseOut(vInt1, vInt2)
    }

    private fun writeTradingCloseOut(w: PacketWriter, m: TradingCloseOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
