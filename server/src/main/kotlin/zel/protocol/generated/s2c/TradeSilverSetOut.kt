package zel.protocol.generated

data class TradeSilverSetOut(
    val int1: Int,
    val int2: Int,
)

object TradeSilverSetOutCodec : OutgoingCodec<TradeSilverSetOut> {
    override val name = "TradeSilverSetOut"

    override fun read(r: PacketReader): TradeSilverSetOut = readTradeSilverSetOut(r)

    override fun write(w: PacketWriter, m: TradeSilverSetOut) = writeTradeSilverSetOut(w, m)

    private fun readTradeSilverSetOut(r: PacketReader): TradeSilverSetOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return TradeSilverSetOut(vInt1, vInt2)
    }

    private fun writeTradeSilverSetOut(w: PacketWriter, m: TradeSilverSetOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
