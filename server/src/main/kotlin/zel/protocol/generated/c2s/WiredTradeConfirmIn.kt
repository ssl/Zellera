package zel.protocol.generated

data class WiredTradeConfirmIn(
    val confirm: Boolean,
)

object WiredTradeConfirmInCodec : IncomingCodec<WiredTradeConfirmIn> {
    override val name = "WiredTradeConfirmIn"

    override fun read(r: PacketReader): WiredTradeConfirmIn = readWiredTradeConfirmIn(r)

    override fun write(w: PacketWriter, m: WiredTradeConfirmIn) = writeWiredTradeConfirmIn(w, m)

    private fun readWiredTradeConfirmIn(r: PacketReader): WiredTradeConfirmIn {
        val vConfirm = r.boolean()
        return WiredTradeConfirmIn(vConfirm)
    }

    private fun writeWiredTradeConfirmIn(w: PacketWriter, m: WiredTradeConfirmIn) {
        w.boolean(m.confirm)
    }
}
