package zel.protocol.generated

data class RecycleItemsIn(
    val itemIds: List<Int>,
)

object RecycleItemsInCodec : IncomingCodec<RecycleItemsIn> {
    override val name = "RecycleItemsIn"

    override fun read(r: PacketReader): RecycleItemsIn = readRecycleItemsIn(r)

    override fun write(w: PacketWriter, m: RecycleItemsIn) = writeRecycleItemsIn(w, m)

    private fun readRecycleItemsIn(r: PacketReader): RecycleItemsIn {
        val vItemIds = List(r.countInt(4)) { r.int() }
        return RecycleItemsIn(vItemIds)
    }

    private fun writeRecycleItemsIn(w: PacketWriter, m: RecycleItemsIn) {
        w.countInt(m.itemIds.size)
        m.itemIds.forEach { w.int(it) }
    }
}
