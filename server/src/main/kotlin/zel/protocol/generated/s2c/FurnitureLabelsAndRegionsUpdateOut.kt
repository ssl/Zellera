package zel.protocol.generated

data class FurnitureLabelsAndRegionsUpdateOut(
    val list1: List<List1Item>,
    val list2: List<Int>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val string1: String,
        val int3: Int,
        val int4: Int,
        val boolean1: Boolean,
    )
}

object FurnitureLabelsAndRegionsUpdateOutCodec : OutgoingCodec<FurnitureLabelsAndRegionsUpdateOut> {
    override val name = "FurnitureLabelsAndRegionsUpdateOut"

    override fun read(r: PacketReader): FurnitureLabelsAndRegionsUpdateOut = readFurnitureLabelsAndRegionsUpdateOut(r)

    override fun write(w: PacketWriter, m: FurnitureLabelsAndRegionsUpdateOut) = writeFurnitureLabelsAndRegionsUpdateOut(w, m)

    private fun readFurnitureLabelsAndRegionsUpdateOutList1Item(r: PacketReader): FurnitureLabelsAndRegionsUpdateOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vBoolean1 = r.boolean()
        return FurnitureLabelsAndRegionsUpdateOut.List1Item(vInt1, vInt2, vString1, vInt3, vInt4, vBoolean1)
    }

    private fun readFurnitureLabelsAndRegionsUpdateOut(r: PacketReader): FurnitureLabelsAndRegionsUpdateOut {
        val vList1 = List(r.countInt(19)) { readFurnitureLabelsAndRegionsUpdateOutList1Item(r) }
        val vList2 = List(r.countInt(4)) { r.int() }
        return FurnitureLabelsAndRegionsUpdateOut(vList1, vList2)
    }

    private fun writeFurnitureLabelsAndRegionsUpdateOutList1Item(w: PacketWriter, m: FurnitureLabelsAndRegionsUpdateOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.int(m.int4)
        w.boolean(m.boolean1)
    }

    private fun writeFurnitureLabelsAndRegionsUpdateOut(w: PacketWriter, m: FurnitureLabelsAndRegionsUpdateOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeFurnitureLabelsAndRegionsUpdateOutList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { w.int(it) }
    }
}
