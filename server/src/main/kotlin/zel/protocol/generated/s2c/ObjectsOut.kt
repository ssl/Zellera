package zel.protocol.generated

data class ObjectsOut(
    val list1: List<List1Item>,
    val list2: List<List2Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
    )

    data class List2Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
        val int4: Int,
        val int5: Int,
        val string1: String,
        val string2: String,
        val int6: Int,
        val stuff1: StuffData,
        val int7: Int,
        val int8: Int,
        val int9: Int,
        val branch1: Branch1,
    ) {
        sealed interface Branch1 {
            data class Then(
                val string1: String,
            ) : Branch1
            data object Else : Branch1
        }
    }
}

object ObjectsOutCodec : OutgoingCodec<ObjectsOut> {
    override val name = "ObjectsOut"

    override fun read(r: PacketReader): ObjectsOut = readObjectsOut(r)

    override fun write(w: PacketWriter, m: ObjectsOut) = writeObjectsOut(w, m)

    private fun readObjectsOutList1Item(r: PacketReader): ObjectsOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return ObjectsOut.List1Item(vInt1, vString1)
    }

    private fun readObjectsOutList2ItemBranch1Then(r: PacketReader): ObjectsOut.List2Item.Branch1.Then {
        val vString1 = r.string()
        return ObjectsOut.List2Item.Branch1.Then(vString1)
    }

    private fun readObjectsOutList2Item(r: PacketReader): ObjectsOut.List2Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt6 = r.int()
        val vStuff1 = StuffData.read(r)
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vInt9 = r.int()
        val vBranch1 = if (vInt2 < 0) readObjectsOutList2ItemBranch1Then(r) else ObjectsOut.List2Item.Branch1.Else
        return ObjectsOut.List2Item(vInt1, vInt2, vInt3, vInt4, vInt5, vString1, vString2, vInt6, vStuff1, vInt7, vInt8, vInt9, vBranch1)
    }

    private fun readObjectsOut(r: PacketReader): ObjectsOut {
        val vList1 = List(r.countInt(6)) { readObjectsOutList1Item(r) }
        val vList2 = List(r.countInt(44)) { readObjectsOutList2Item(r) }
        return ObjectsOut(vList1, vList2)
    }

    private fun writeObjectsOutList1Item(w: PacketWriter, m: ObjectsOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeObjectsOutList2ItemBranch1Then(w: PacketWriter, m: ObjectsOut.List2Item.Branch1.Then) {
        w.string(m.string1)
    }

    private fun writeObjectsOutList2Item(w: PacketWriter, m: ObjectsOut.List2Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int6)
        StuffData.write(w, m.stuff1)
        w.int(m.int7)
        w.int(m.int8)
        w.int(m.int9)
        when (val b = m.branch1) {
            is ObjectsOut.List2Item.Branch1.Then -> writeObjectsOutList2ItemBranch1Then(w, b)
            is ObjectsOut.List2Item.Branch1.Else -> {}
        }
    }

    private fun writeObjectsOut(w: PacketWriter, m: ObjectsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeObjectsOutList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { writeObjectsOutList2Item(w, it) }
    }
}
