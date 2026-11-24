package zel.protocol.generated

data object WiredTradeCompletedOut

object WiredTradeCompletedOutCodec : OutgoingCodec<WiredTradeCompletedOut> {
    override val name = "WiredTradeCompletedOut"

    override fun read(r: PacketReader): WiredTradeCompletedOut = WiredTradeCompletedOut

    override fun write(w: PacketWriter, m: WiredTradeCompletedOut) = writeWiredTradeCompletedOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeWiredTradeCompletedOut(w: PacketWriter, m: WiredTradeCompletedOut) {
    }
}
