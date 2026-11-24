package zel.protocol.generated

data class WiredFurniVisualizationStateOut(
    val list1: List<List1Item>,
    val list2: List<List2Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val boolean1: Boolean,
        val int3: Int,
    )

    data class List2Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
        val int4: Int,
        val int5: Int,
        val int6: Int,
    )
}

object WiredFurniVisualizationStateOutCodec : OutgoingCodec<WiredFurniVisualizationStateOut> {
    override val name = "WiredFurniVisualizationStateOut"

    override fun read(r: PacketReader): WiredFurniVisualizationStateOut = readWiredFurniVisualizationStateOut(r)

    override fun write(w: PacketWriter, m: WiredFurniVisualizationStateOut) = writeWiredFurniVisualizationStateOut(w, m)

    private fun readWiredFurniVisualizationStateOutList1Item(r: PacketReader): WiredFurniVisualizationStateOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        val vInt3 = r.int()
        return WiredFurniVisualizationStateOut.List1Item(vInt1, vInt2, vBoolean1, vInt3)
    }

    private fun readWiredFurniVisualizationStateOutList2Item(r: PacketReader): WiredFurniVisualizationStateOut.List2Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        return WiredFurniVisualizationStateOut.List2Item(vInt1, vInt2, vInt3, vInt4, vInt5, vInt6)
    }

    private fun readWiredFurniVisualizationStateOut(r: PacketReader): WiredFurniVisualizationStateOut {
        val vList1 = List(r.countInt(13)) { readWiredFurniVisualizationStateOutList1Item(r) }
        val vList2 = List(r.countInt(24)) { readWiredFurniVisualizationStateOutList2Item(r) }
        return WiredFurniVisualizationStateOut(vList1, vList2)
    }

    private fun writeWiredFurniVisualizationStateOutList1Item(w: PacketWriter, m: WiredFurniVisualizationStateOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.boolean(m.boolean1)
        w.int(m.int3)
    }

    private fun writeWiredFurniVisualizationStateOutList2Item(w: PacketWriter, m: WiredFurniVisualizationStateOut.List2Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
    }

    private fun writeWiredFurniVisualizationStateOut(w: PacketWriter, m: WiredFurniVisualizationStateOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeWiredFurniVisualizationStateOutList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { writeWiredFurniVisualizationStateOutList2Item(w, it) }
    }
}
