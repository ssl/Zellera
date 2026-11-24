package zel.protocol.generated

data class AddItemToTradeIn(
    val itemId: Int,
)

object AddItemToTradeInCodec : IncomingCodec<AddItemToTradeIn> {
    override val name = "AddItemToTradeIn"

    override fun read(r: PacketReader): AddItemToTradeIn = readAddItemToTradeIn(r)

    override fun write(w: PacketWriter, m: AddItemToTradeIn) = writeAddItemToTradeIn(w, m)

    private fun readAddItemToTradeIn(r: PacketReader): AddItemToTradeIn {
        val vItemId = r.int()
        return AddItemToTradeIn(vItemId)
    }

    private fun writeAddItemToTradeIn(w: PacketWriter, m: AddItemToTradeIn) {
        w.int(m.itemId)
    }
}
