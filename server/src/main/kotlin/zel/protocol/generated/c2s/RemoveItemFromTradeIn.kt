package zel.protocol.generated

data class RemoveItemFromTradeIn(
    val itemId: Int,
)

object RemoveItemFromTradeInCodec : IncomingCodec<RemoveItemFromTradeIn> {
    override val name = "RemoveItemFromTradeIn"

    override fun read(r: PacketReader): RemoveItemFromTradeIn = readRemoveItemFromTradeIn(r)

    override fun write(w: PacketWriter, m: RemoveItemFromTradeIn) = writeRemoveItemFromTradeIn(w, m)

    private fun readRemoveItemFromTradeIn(r: PacketReader): RemoveItemFromTradeIn {
        val vItemId = r.int()
        return RemoveItemFromTradeIn(vItemId)
    }

    private fun writeRemoveItemFromTradeIn(w: PacketWriter, m: RemoveItemFromTradeIn) {
        w.int(m.itemId)
    }
}
