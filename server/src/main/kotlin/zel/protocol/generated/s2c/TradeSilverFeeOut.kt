package zel.protocol.generated

data class TradeSilverFeeOut(
    val int1: Int,
)

object TradeSilverFeeOutCodec : OutgoingCodec<TradeSilverFeeOut> {
    override val name = "TradeSilverFeeOut"

    override fun read(r: PacketReader): TradeSilverFeeOut = readTradeSilverFeeOut(r)

    override fun write(w: PacketWriter, m: TradeSilverFeeOut) = writeTradeSilverFeeOut(w, m)

    private fun readTradeSilverFeeOut(r: PacketReader): TradeSilverFeeOut {
        val vInt1 = r.int()
        return TradeSilverFeeOut(vInt1)
    }

    private fun writeTradeSilverFeeOut(w: PacketWriter, m: TradeSilverFeeOut) {
        w.int(m.int1)
    }
}
