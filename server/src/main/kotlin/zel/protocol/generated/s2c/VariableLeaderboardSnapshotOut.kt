package zel.protocol.generated

data class VariableLeaderboardSnapshotOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val string1: String,
        val string2: String,
        val int3: Int,
        val int4: Int,
        val int5: Int,
        val int6: Int,
        val int7: Int,
        val list1: List<List1Item>,
        val list2: List<List2Item>,
        val string3: String,
        val int8: Int,
    ) {
        data class List1Item(
            val int1: Int,
            val string1: String,
            val int2: Int,
        )

        data class List2Item(
            val string1: String,
            val string2: String,
        )
    }
}

object VariableLeaderboardSnapshotOutCodec : OutgoingCodec<VariableLeaderboardSnapshotOut> {
    override val name = "VariableLeaderboardSnapshotOut"

    override fun read(r: PacketReader): VariableLeaderboardSnapshotOut = readVariableLeaderboardSnapshotOut(r)

    override fun write(w: PacketWriter, m: VariableLeaderboardSnapshotOut) = writeVariableLeaderboardSnapshotOut(w, m)

    private fun readVariableLeaderboardSnapshotOutList1ItemList1Item(r: PacketReader): VariableLeaderboardSnapshotOut.List1Item.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        return VariableLeaderboardSnapshotOut.List1Item.List1Item(vInt1, vString1, vInt2)
    }

    private fun readVariableLeaderboardSnapshotOutList1ItemList2Item(r: PacketReader): VariableLeaderboardSnapshotOut.List1Item.List2Item {
        val vString1 = r.string()
        val vString2 = r.string()
        return VariableLeaderboardSnapshotOut.List1Item.List2Item(vString1, vString2)
    }

    private fun readVariableLeaderboardSnapshotOutList1Item(r: PacketReader): VariableLeaderboardSnapshotOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vList1 = List(r.countInt(10)) { readVariableLeaderboardSnapshotOutList1ItemList1Item(r) }
        val vList2 = List(r.countInt(4)) { readVariableLeaderboardSnapshotOutList1ItemList2Item(r) }
        val vString3 = r.string()
        val vInt8 = r.int()
        return VariableLeaderboardSnapshotOut.List1Item(vInt1, vInt2, vString1, vString2, vInt3, vInt4, vInt5, vInt6, vInt7, vList1, vList2, vString3, vInt8)
    }

    private fun readVariableLeaderboardSnapshotOut(r: PacketReader): VariableLeaderboardSnapshotOut {
        val vList1 = List(r.countInt(46)) { readVariableLeaderboardSnapshotOutList1Item(r) }
        return VariableLeaderboardSnapshotOut(vList1)
    }

    private fun writeVariableLeaderboardSnapshotOutList1ItemList1Item(w: PacketWriter, m: VariableLeaderboardSnapshotOut.List1Item.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
    }

    private fun writeVariableLeaderboardSnapshotOutList1ItemList2Item(w: PacketWriter, m: VariableLeaderboardSnapshotOut.List1Item.List2Item) {
        w.string(m.string1)
        w.string(m.string2)
    }

    private fun writeVariableLeaderboardSnapshotOutList1Item(w: PacketWriter, m: VariableLeaderboardSnapshotOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.countInt(m.list1.size)
        m.list1.forEach { writeVariableLeaderboardSnapshotOutList1ItemList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { writeVariableLeaderboardSnapshotOutList1ItemList2Item(w, it) }
        w.string(m.string3)
        w.int(m.int8)
    }

    private fun writeVariableLeaderboardSnapshotOut(w: PacketWriter, m: VariableLeaderboardSnapshotOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeVariableLeaderboardSnapshotOutList1Item(w, it) }
    }
}
