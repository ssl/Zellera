package zel.protocol.generated

data class WiredTradeCancelledOut(
    val int1: Int,
)

object WiredTradeCancelledOutCodec : OutgoingCodec<WiredTradeCancelledOut> {
    override val name = "WiredTradeCancelledOut"

    override fun read(r: PacketReader): WiredTradeCancelledOut = readWiredTradeCancelledOut(r)

    override fun write(w: PacketWriter, m: WiredTradeCancelledOut) = writeWiredTradeCancelledOut(w, m)

    private fun readWiredTradeCancelledOut(r: PacketReader): WiredTradeCancelledOut {
        val vInt1 = r.int()
        return WiredTradeCancelledOut(vInt1)
    }

    private fun writeWiredTradeCancelledOut(w: PacketWriter, m: WiredTradeCancelledOut) {
        w.int(m.int1)
    }
}
