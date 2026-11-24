package zel.protocol.generated

data class RemoveItemIn(
    val itemId: Int,
)

object RemoveItemInCodec : IncomingCodec<RemoveItemIn> {
    override val name = "RemoveItemIn"

    override fun read(r: PacketReader): RemoveItemIn = readRemoveItemIn(r)

    override fun write(w: PacketWriter, m: RemoveItemIn) = writeRemoveItemIn(w, m)

    private fun readRemoveItemIn(r: PacketReader): RemoveItemIn {
        val vItemId = r.int()
        return RemoveItemIn(vItemId)
    }

    private fun writeRemoveItemIn(w: PacketWriter, m: RemoveItemIn) {
        w.int(m.itemId)
    }
}
