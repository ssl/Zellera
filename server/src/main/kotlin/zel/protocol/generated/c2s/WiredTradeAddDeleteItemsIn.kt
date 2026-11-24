package zel.protocol.generated

data class WiredTradeAddDeleteItemsIn(
    val add: Boolean,
    val itemIds: List<Int>,
)

object WiredTradeAddDeleteItemsInCodec : IncomingCodec<WiredTradeAddDeleteItemsIn> {
    override val name = "WiredTradeAddDeleteItemsIn"

    override fun read(r: PacketReader): WiredTradeAddDeleteItemsIn = readWiredTradeAddDeleteItemsIn(r)

    override fun write(w: PacketWriter, m: WiredTradeAddDeleteItemsIn) = writeWiredTradeAddDeleteItemsIn(w, m)

    private fun readWiredTradeAddDeleteItemsIn(r: PacketReader): WiredTradeAddDeleteItemsIn {
        val vAdd = r.boolean()
        val vItemIds = List(r.countInt(4)) { r.int() }
        return WiredTradeAddDeleteItemsIn(vAdd, vItemIds)
    }

    private fun writeWiredTradeAddDeleteItemsIn(w: PacketWriter, m: WiredTradeAddDeleteItemsIn) {
        w.boolean(m.add)
        w.countInt(m.itemIds.size)
        m.itemIds.forEach { w.int(it) }
    }
}
