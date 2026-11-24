package zel.protocol.generated

data class WiredLinkOpenIn(
    val furniId: Int,
)

object WiredLinkOpenInCodec : IncomingCodec<WiredLinkOpenIn> {
    override val name = "WiredLinkOpenIn"

    override fun read(r: PacketReader): WiredLinkOpenIn = readWiredLinkOpenIn(r)

    override fun write(w: PacketWriter, m: WiredLinkOpenIn) = writeWiredLinkOpenIn(w, m)

    private fun readWiredLinkOpenIn(r: PacketReader): WiredLinkOpenIn {
        val vFurniId = r.int()
        return WiredLinkOpenIn(vFurniId)
    }

    private fun writeWiredLinkOpenIn(w: PacketWriter, m: WiredLinkOpenIn) {
        w.int(m.furniId)
    }
}
