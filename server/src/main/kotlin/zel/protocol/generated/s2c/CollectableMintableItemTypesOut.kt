package zel.protocol.generated

data class CollectableMintableItemTypesOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
        val boolean1: Boolean,
        val int4: Int,
        val boolean2: Boolean,
        val short1: Short,
    )
}

object CollectableMintableItemTypesOutCodec : OutgoingCodec<CollectableMintableItemTypesOut> {
    override val name = "CollectableMintableItemTypesOut"

    override fun read(r: PacketReader): CollectableMintableItemTypesOut = readCollectableMintableItemTypesOut(r)

    override fun write(w: PacketWriter, m: CollectableMintableItemTypesOut) = writeCollectableMintableItemTypesOut(w, m)

    private fun readCollectableMintableItemTypesOutList1Item(r: PacketReader): CollectableMintableItemTypesOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vBoolean1 = r.boolean()
        val vInt4 = r.int()
        val vBoolean2 = r.boolean()
        val vShort1 = r.short()
        return CollectableMintableItemTypesOut.List1Item(vInt1, vInt2, vInt3, vBoolean1, vInt4, vBoolean2, vShort1)
    }

    private fun readCollectableMintableItemTypesOut(r: PacketReader): CollectableMintableItemTypesOut {
        val vList1 = List(r.countInt(20)) { readCollectableMintableItemTypesOutList1Item(r) }
        return CollectableMintableItemTypesOut(vList1)
    }

    private fun writeCollectableMintableItemTypesOutList1Item(w: PacketWriter, m: CollectableMintableItemTypesOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.boolean(m.boolean1)
        w.int(m.int4)
        w.boolean(m.boolean2)
        w.short(m.short1)
    }

    private fun writeCollectableMintableItemTypesOut(w: PacketWriter, m: CollectableMintableItemTypesOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeCollectableMintableItemTypesOutList1Item(w, it) }
    }
}
