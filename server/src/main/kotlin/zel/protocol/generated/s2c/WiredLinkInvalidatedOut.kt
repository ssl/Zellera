package zel.protocol.generated

data class WiredLinkInvalidatedOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
)

object WiredLinkInvalidatedOutCodec : OutgoingCodec<WiredLinkInvalidatedOut> {
    override val name = "WiredLinkInvalidatedOut"

    override fun read(r: PacketReader): WiredLinkInvalidatedOut = readWiredLinkInvalidatedOut(r)

    override fun write(w: PacketWriter, m: WiredLinkInvalidatedOut) = writeWiredLinkInvalidatedOut(w, m)

    private fun readWiredLinkInvalidatedOut(r: PacketReader): WiredLinkInvalidatedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        return WiredLinkInvalidatedOut(vInt1, vInt2, vString1)
    }

    private fun writeWiredLinkInvalidatedOut(w: PacketWriter, m: WiredLinkInvalidatedOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
    }
}
