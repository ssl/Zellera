package zel.protocol.generated

data class AddItemsToTradeIn(
    val itemIds: List<Int>,
)

object AddItemsToTradeInCodec : IncomingCodec<AddItemsToTradeIn> {
    override val name = "AddItemsToTradeIn"

    override fun read(r: PacketReader): AddItemsToTradeIn = readAddItemsToTradeIn(r)

    override fun write(w: PacketWriter, m: AddItemsToTradeIn) = writeAddItemsToTradeIn(w, m)

    private fun readAddItemsToTradeIn(r: PacketReader): AddItemsToTradeIn {
        val vItemIds = List(r.countInt(4)) { r.int() }
        return AddItemsToTradeIn(vItemIds)
    }

    private fun writeAddItemsToTradeIn(w: PacketWriter, m: AddItemsToTradeIn) {
        w.countInt(m.itemIds.size)
        m.itemIds.forEach { w.int(it) }
    }
}
