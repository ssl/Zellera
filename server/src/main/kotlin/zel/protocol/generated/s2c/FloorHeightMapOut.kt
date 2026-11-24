package zel.protocol.generated

data class FloorHeightMapOut(
    val boolean1: Boolean,
    val int1: Int,
    val string1: String,
    val list1: List<List1Item>,
    val int2: Int,
    val int3: Int,
    val float1: Float,
) {
    data class List1Item(
        val int1: Int,
        val boolean1: Boolean,
        val int2: Int,
        val int3: Int,
        val int4: Int,
        val int5: Int,
        val boolean2: Boolean,
    )
}

object FloorHeightMapOutCodec : OutgoingCodec<FloorHeightMapOut> {
    override val name = "FloorHeightMapOut"

    override fun read(r: PacketReader): FloorHeightMapOut = readFloorHeightMapOut(r)

    override fun write(w: PacketWriter, m: FloorHeightMapOut) = writeFloorHeightMapOut(w, m)

    private fun readFloorHeightMapOutList1Item(r: PacketReader): FloorHeightMapOut.List1Item {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vBoolean2 = r.boolean()
        return FloorHeightMapOut.List1Item(vInt1, vBoolean1, vInt2, vInt3, vInt4, vInt5, vBoolean2)
    }

    private fun readFloorHeightMapOut(r: PacketReader): FloorHeightMapOut {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vString1 = r.string()
        val vList1 = List(r.countInt(22)) { readFloorHeightMapOutList1Item(r) }
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vFloat1 = r.float()
        return FloorHeightMapOut(vBoolean1, vInt1, vString1, vList1, vInt2, vInt3, vFloat1)
    }

    private fun writeFloorHeightMapOutList1Item(w: PacketWriter, m: FloorHeightMapOut.List1Item) {
        w.int(m.int1)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.boolean(m.boolean2)
    }

    private fun writeFloorHeightMapOut(w: PacketWriter, m: FloorHeightMapOut) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeFloorHeightMapOutList1Item(w, it) }
        w.int(m.int2)
        w.int(m.int3)
        w.float(m.float1)
    }
}
