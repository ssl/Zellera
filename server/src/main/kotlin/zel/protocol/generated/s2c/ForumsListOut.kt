package zel.protocol.generated

data class ForumsListOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val string2: String,
        val string3: String,
        val int2: Int,
        val int3: Int,
        val int4: Int,
        val int5: Int,
        val int6: Int,
        val int7: Int,
        val string4: String,
        val int8: Int,
    )
}

object ForumsListOutCodec : OutgoingCodec<ForumsListOut> {
    override val name = "ForumsListOut"

    override fun read(r: PacketReader): ForumsListOut = readForumsListOut(r)

    override fun write(w: PacketWriter, m: ForumsListOut) = writeForumsListOut(w, m)

    private fun readForumsListOutList1Item(r: PacketReader): ForumsListOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vString4 = r.string()
        val vInt8 = r.int()
        return ForumsListOut.List1Item(vInt1, vString1, vString2, vString3, vInt2, vInt3, vInt4, vInt5, vInt6, vInt7, vString4, vInt8)
    }

    private fun readForumsListOut(r: PacketReader): ForumsListOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vList1 = List(r.countInt(40)) { readForumsListOutList1Item(r) }
        return ForumsListOut(vInt1, vInt2, vInt3, vList1)
    }

    private fun writeForumsListOutList1Item(w: PacketWriter, m: ForumsListOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.string(m.string4)
        w.int(m.int8)
    }

    private fun writeForumsListOut(w: PacketWriter, m: ForumsListOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.countInt(m.list1.size)
        m.list1.forEach { writeForumsListOutList1Item(w, it) }
    }
}
