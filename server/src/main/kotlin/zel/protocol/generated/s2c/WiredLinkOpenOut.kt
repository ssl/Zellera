package zel.protocol.generated

data class WiredLinkOpenOut(
    val int1: Int,
)

object WiredLinkOpenOutCodec : OutgoingCodec<WiredLinkOpenOut> {
    override val name = "WiredLinkOpenOut"

    override fun read(r: PacketReader): WiredLinkOpenOut = readWiredLinkOpenOut(r)

    override fun write(w: PacketWriter, m: WiredLinkOpenOut) = writeWiredLinkOpenOut(w, m)

    private fun readWiredLinkOpenOut(r: PacketReader): WiredLinkOpenOut {
        val vInt1 = r.int()
        return WiredLinkOpenOut(vInt1)
    }

    private fun writeWiredLinkOpenOut(w: PacketWriter, m: WiredLinkOpenOut) {
        w.int(m.int1)
    }
}
