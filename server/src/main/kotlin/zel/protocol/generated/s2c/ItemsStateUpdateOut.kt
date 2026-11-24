package zel.protocol.generated

data class ItemsStateUpdateOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
    )
}

object ItemsStateUpdateOutCodec : OutgoingCodec<ItemsStateUpdateOut> {
    override val name = "ItemsStateUpdateOut"

    override fun read(r: PacketReader): ItemsStateUpdateOut = readItemsStateUpdateOut(r)

    override fun write(w: PacketWriter, m: ItemsStateUpdateOut) = writeItemsStateUpdateOut(w, m)

    private fun readItemsStateUpdateOutList1Item(r: PacketReader): ItemsStateUpdateOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return ItemsStateUpdateOut.List1Item(vInt1, vString1)
    }

    private fun readItemsStateUpdateOut(r: PacketReader): ItemsStateUpdateOut {
        val vList1 = List(r.countInt(6)) { readItemsStateUpdateOutList1Item(r) }
        return ItemsStateUpdateOut(vList1)
    }

    private fun writeItemsStateUpdateOutList1Item(w: PacketWriter, m: ItemsStateUpdateOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeItemsStateUpdateOut(w: PacketWriter, m: ItemsStateUpdateOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeItemsStateUpdateOutList1Item(w, it) }
    }
}
