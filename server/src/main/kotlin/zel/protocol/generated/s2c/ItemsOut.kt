package zel.protocol.generated

data class ItemsOut(
    val list1: List<List1Item>,
    val list2: List<List2Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
    )

    data class List2Item(
        val string1: String,
        val int1: Int,
        val string2: String,
        val string3: String,
        val int2: Int,
        val int3: Int,
        val int4: Int,
    )
}

object ItemsOutCodec : OutgoingCodec<ItemsOut> {
    override val name = "ItemsOut"

    override fun read(r: PacketReader): ItemsOut = readItemsOut(r)

    override fun write(w: PacketWriter, m: ItemsOut) = writeItemsOut(w, m)

    private fun readItemsOutList1Item(r: PacketReader): ItemsOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return ItemsOut.List1Item(vInt1, vString1)
    }

    private fun readItemsOutList2Item(r: PacketReader): ItemsOut.List2Item {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        return ItemsOut.List2Item(vString1, vInt1, vString2, vString3, vInt2, vInt3, vInt4)
    }

    private fun readItemsOut(r: PacketReader): ItemsOut {
        val vList1 = List(r.countInt(6)) { readItemsOutList1Item(r) }
        val vList2 = List(r.countInt(22)) { readItemsOutList2Item(r) }
        return ItemsOut(vList1, vList2)
    }

    private fun writeItemsOutList1Item(w: PacketWriter, m: ItemsOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeItemsOutList2Item(w: PacketWriter, m: ItemsOut.List2Item) {
        w.string(m.string1)
        w.int(m.int1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
    }

    private fun writeItemsOut(w: PacketWriter, m: ItemsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeItemsOutList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { writeItemsOutList2Item(w, it) }
    }
}
