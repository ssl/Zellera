package zel.protocol.generated

data class OpenIn(
    val itemId: Int,
)

object OpenInCodec : IncomingCodec<OpenIn> {
    override val name = "OpenIn"

    override fun read(r: PacketReader): OpenIn = readOpenIn(r)

    override fun write(w: PacketWriter, m: OpenIn) = writeOpenIn(w, m)

    private fun readOpenIn(r: PacketReader): OpenIn {
        val vItemId = r.int()
        return OpenIn(vItemId)
    }

    private fun writeOpenIn(w: PacketWriter, m: OpenIn) {
        w.int(m.itemId)
    }
}
