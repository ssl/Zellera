package zel.protocol.generated

data class TradingAcceptOut(
    val int1: Int,
    val int2: Int,
)

object TradingAcceptOutCodec : OutgoingCodec<TradingAcceptOut> {
    override val name = "TradingAcceptOut"

    override fun read(r: PacketReader): TradingAcceptOut = readTradingAcceptOut(r)

    override fun write(w: PacketWriter, m: TradingAcceptOut) = writeTradingAcceptOut(w, m)

    private fun readTradingAcceptOut(r: PacketReader): TradingAcceptOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return TradingAcceptOut(vInt1, vInt2)
    }

    private fun writeTradingAcceptOut(w: PacketWriter, m: TradingAcceptOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
