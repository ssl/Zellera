package zel.protocol.generated

data class WiredPlayerVisualizationStateOut(
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

object WiredPlayerVisualizationStateOutCodec : OutgoingCodec<WiredPlayerVisualizationStateOut> {
    override val name = "WiredPlayerVisualizationStateOut"

    override fun read(r: PacketReader): WiredPlayerVisualizationStateOut = readWiredPlayerVisualizationStateOut(r)

    override fun write(w: PacketWriter, m: WiredPlayerVisualizationStateOut) = writeWiredPlayerVisualizationStateOut(w, m)

    private fun readWiredPlayerVisualizationStateOutList1Item(r: PacketReader): WiredPlayerVisualizationStateOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        val vInt3 = r.int()
        return WiredPlayerVisualizationStateOut.List1Item(vInt1, vInt2, vBoolean1, vInt3)
    }

    private fun readWiredPlayerVisualizationStateOutList2Item(r: PacketReader): WiredPlayerVisualizationStateOut.List2Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        return WiredPlayerVisualizationStateOut.List2Item(vInt1, vInt2, vInt3, vInt4, vInt5, vInt6)
    }

    private fun readWiredPlayerVisualizationStateOut(r: PacketReader): WiredPlayerVisualizationStateOut {
        val vList1 = List(r.countInt(13)) { readWiredPlayerVisualizationStateOutList1Item(r) }
        val vList2 = List(r.countInt(24)) { readWiredPlayerVisualizationStateOutList2Item(r) }
        return WiredPlayerVisualizationStateOut(vList1, vList2)
    }

    private fun writeWiredPlayerVisualizationStateOutList1Item(w: PacketWriter, m: WiredPlayerVisualizationStateOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.boolean(m.boolean1)
        w.int(m.int3)
    }

    private fun writeWiredPlayerVisualizationStateOutList2Item(w: PacketWriter, m: WiredPlayerVisualizationStateOut.List2Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
    }

    private fun writeWiredPlayerVisualizationStateOut(w: PacketWriter, m: WiredPlayerVisualizationStateOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeWiredPlayerVisualizationStateOutList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { writeWiredPlayerVisualizationStateOutList2Item(w, it) }
    }
}
