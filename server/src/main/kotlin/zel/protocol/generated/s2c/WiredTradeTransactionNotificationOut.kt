package zel.protocol.generated

data class WiredTradeTransactionNotificationOut(
    val int1: Int,
)

object WiredTradeTransactionNotificationOutCodec : OutgoingCodec<WiredTradeTransactionNotificationOut> {
    override val name = "WiredTradeTransactionNotificationOut"

    override fun read(r: PacketReader): WiredTradeTransactionNotificationOut = readWiredTradeTransactionNotificationOut(r)

    override fun write(w: PacketWriter, m: WiredTradeTransactionNotificationOut) = writeWiredTradeTransactionNotificationOut(w, m)

    private fun readWiredTradeTransactionNotificationOut(r: PacketReader): WiredTradeTransactionNotificationOut {
        val vInt1 = r.int()
        return WiredTradeTransactionNotificationOut(vInt1)
    }

    private fun writeWiredTradeTransactionNotificationOut(w: PacketWriter, m: WiredTradeTransactionNotificationOut) {
        w.int(m.int1)
    }
}
