package zel.protocol.generated

data class FurnitureLabelsAndRegionsOut(
    val list1: List<List1Item>,
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

object FurnitureLabelsAndRegionsOutCodec : OutgoingCodec<FurnitureLabelsAndRegionsOut> {
    override val name = "FurnitureLabelsAndRegionsOut"

    override fun read(r: PacketReader): FurnitureLabelsAndRegionsOut = readFurnitureLabelsAndRegionsOut(r)

    override fun write(w: PacketWriter, m: FurnitureLabelsAndRegionsOut) = writeFurnitureLabelsAndRegionsOut(w, m)

    private fun readFurnitureLabelsAndRegionsOutList1Item(r: PacketReader): FurnitureLabelsAndRegionsOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vBoolean1 = r.boolean()
        return FurnitureLabelsAndRegionsOut.List1Item(vInt1, vInt2, vString1, vInt3, vInt4, vBoolean1)
    }

    private fun readFurnitureLabelsAndRegionsOut(r: PacketReader): FurnitureLabelsAndRegionsOut {
        val vList1 = List(r.countInt(19)) { readFurnitureLabelsAndRegionsOutList1Item(r) }
        return FurnitureLabelsAndRegionsOut(vList1)
    }

    private fun writeFurnitureLabelsAndRegionsOutList1Item(w: PacketWriter, m: FurnitureLabelsAndRegionsOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.int(m.int4)
        w.boolean(m.boolean1)
    }

    private fun writeFurnitureLabelsAndRegionsOut(w: PacketWriter, m: FurnitureLabelsAndRegionsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeFurnitureLabelsAndRegionsOutList1Item(w, it) }
    }
}
