package zel.protocol.generated

data class WiredTradeItemsUpdateOut(
    val int1: Int,
    val rest: ByteArray,
)

object WiredTradeItemsUpdateOutCodec : OutgoingCodec<WiredTradeItemsUpdateOut> {
    override val name = "WiredTradeItemsUpdateOut"

    override fun read(r: PacketReader): WiredTradeItemsUpdateOut = readWiredTradeItemsUpdateOut(r)

    override fun write(w: PacketWriter, m: WiredTradeItemsUpdateOut) = writeWiredTradeItemsUpdateOut(w, m)

    private fun readWiredTradeItemsUpdateOut(r: PacketReader): WiredTradeItemsUpdateOut {
        val vInt1 = r.int()
        val vRest = r.rest()
        return WiredTradeItemsUpdateOut(vInt1, vRest)
    }

    private fun writeWiredTradeItemsUpdateOut(w: PacketWriter, m: WiredTradeItemsUpdateOut) {
        w.int(m.int1)
        w.bytes(m.rest)
    }
}
