package zel.protocol.generated

data object WiredTradeCancelIn

object WiredTradeCancelInCodec : IncomingCodec<WiredTradeCancelIn> {
    override val name = "WiredTradeCancelIn"

    override fun read(r: PacketReader): WiredTradeCancelIn = WiredTradeCancelIn

    override fun write(w: PacketWriter, m: WiredTradeCancelIn) = writeWiredTradeCancelIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeWiredTradeCancelIn(w: PacketWriter, m: WiredTradeCancelIn) {
    }
}
