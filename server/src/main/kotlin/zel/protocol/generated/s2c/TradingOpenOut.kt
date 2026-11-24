package zel.protocol.generated

data class TradingOpenOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
)

object TradingOpenOutCodec : OutgoingCodec<TradingOpenOut> {
    override val name = "TradingOpenOut"

    override fun read(r: PacketReader): TradingOpenOut = readTradingOpenOut(r)

    override fun write(w: PacketWriter, m: TradingOpenOut) = writeTradingOpenOut(w, m)

    private fun readTradingOpenOut(r: PacketReader): TradingOpenOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        return TradingOpenOut(vInt1, vInt2, vInt3, vInt4)
    }

    private fun writeTradingOpenOut(w: PacketWriter, m: TradingOpenOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
    }
}
