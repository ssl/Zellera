package zel.protocol.generated

data class WiredMenuErrorOut(
    val short1: Short,
)

object WiredMenuErrorOutCodec : OutgoingCodec<WiredMenuErrorOut> {
    override val name = "WiredMenuErrorOut"

    override fun read(r: PacketReader): WiredMenuErrorOut = readWiredMenuErrorOut(r)

    override fun write(w: PacketWriter, m: WiredMenuErrorOut) = writeWiredMenuErrorOut(w, m)

    private fun readWiredMenuErrorOut(r: PacketReader): WiredMenuErrorOut {
        val vShort1 = r.short()
        return WiredMenuErrorOut(vShort1)
    }

    private fun writeWiredMenuErrorOut(w: PacketWriter, m: WiredMenuErrorOut) {
        w.short(m.short1)
    }
}
