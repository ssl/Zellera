package zel.protocol.generated

data class TradeOpenFailedOut(
    val int1: Int,
    val string1: String,
)

object TradeOpenFailedOutCodec : OutgoingCodec<TradeOpenFailedOut> {
    override val name = "TradeOpenFailedOut"

    override fun read(r: PacketReader): TradeOpenFailedOut = readTradeOpenFailedOut(r)

    override fun write(w: PacketWriter, m: TradeOpenFailedOut) = writeTradeOpenFailedOut(w, m)

    private fun readTradeOpenFailedOut(r: PacketReader): TradeOpenFailedOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        return TradeOpenFailedOut(vInt1, vString1)
    }

    private fun writeTradeOpenFailedOut(w: PacketWriter, m: TradeOpenFailedOut) {
        w.int(m.int1)
        w.string(m.string1)
    }
}
