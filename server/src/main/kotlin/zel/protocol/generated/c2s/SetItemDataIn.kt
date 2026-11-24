package zel.protocol.generated

data class SetItemDataIn(
    val itemId: Int,
    val color: String,
    val text: String,
)

object SetItemDataInCodec : IncomingCodec<SetItemDataIn> {
    override val name = "SetItemDataIn"

    override fun read(r: PacketReader): SetItemDataIn = readSetItemDataIn(r)

    override fun write(w: PacketWriter, m: SetItemDataIn) = writeSetItemDataIn(w, m)

    private fun readSetItemDataIn(r: PacketReader): SetItemDataIn {
        val vItemId = r.int()
        val vColor = r.string()
        val vText = r.string()
        return SetItemDataIn(vItemId, vColor, vText)
    }

    private fun writeSetItemDataIn(w: PacketWriter, m: SetItemDataIn) {
        w.int(m.itemId)
        w.string(m.color)
        w.string(m.text)
    }
}
