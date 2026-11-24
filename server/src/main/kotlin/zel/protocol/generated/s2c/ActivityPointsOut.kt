package zel.protocol.generated

data class ActivityPointsOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
    )
}

object ActivityPointsOutCodec : OutgoingCodec<ActivityPointsOut> {
    override val name = "ActivityPointsOut"

    override fun read(r: PacketReader): ActivityPointsOut = readActivityPointsOut(r)

    override fun write(w: PacketWriter, m: ActivityPointsOut) = writeActivityPointsOut(w, m)

    private fun readActivityPointsOutList1Item(r: PacketReader): ActivityPointsOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return ActivityPointsOut.List1Item(vInt1, vInt2)
    }

    private fun readActivityPointsOut(r: PacketReader): ActivityPointsOut {
        val vList1 = List(r.countInt(8)) { readActivityPointsOutList1Item(r) }
        return ActivityPointsOut(vList1)
    }

    private fun writeActivityPointsOutList1Item(w: PacketWriter, m: ActivityPointsOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writeActivityPointsOut(w: PacketWriter, m: ActivityPointsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeActivityPointsOutList1Item(w, it) }
    }
}
