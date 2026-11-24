package zel.protocol.generated

data class ItemsChestContentsUpdatedOut(
    val int1: Int,
    val list1: List<Int>,
    val rest: ByteArray,
)

object ItemsChestContentsUpdatedOutCodec : OutgoingCodec<ItemsChestContentsUpdatedOut> {
    override val name = "ItemsChestContentsUpdatedOut"

    override fun read(r: PacketReader): ItemsChestContentsUpdatedOut = readItemsChestContentsUpdatedOut(r)

    override fun write(w: PacketWriter, m: ItemsChestContentsUpdatedOut) = writeItemsChestContentsUpdatedOut(w, m)

    private fun readItemsChestContentsUpdatedOut(r: PacketReader): ItemsChestContentsUpdatedOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(4)) { r.int() }
        val vRest = r.rest()
        return ItemsChestContentsUpdatedOut(vInt1, vList1, vRest)
    }

    private fun writeItemsChestContentsUpdatedOut(w: PacketWriter, m: ItemsChestContentsUpdatedOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.bytes(m.rest)
    }
}
