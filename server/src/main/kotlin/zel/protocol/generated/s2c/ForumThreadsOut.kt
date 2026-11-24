package zel.protocol.generated

data class ForumThreadsOut(
    val int1: Int,
    val int2: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val string1: String,
        val string2: String,
        val boolean1: Boolean,
        val boolean2: Boolean,
        val int3: Int,
        val int4: Int,
        val int5: Int,
        val int6: Int,
        val int7: Int,
        val string3: String,
        val int8: Int,
        val byte1: Byte,
        val int9: Int,
        val string4: String,
        val int10: Int,
    )
}

object ForumThreadsOutCodec : OutgoingCodec<ForumThreadsOut> {
    override val name = "ForumThreadsOut"

    override fun read(r: PacketReader): ForumThreadsOut = readForumThreadsOut(r)

    override fun write(w: PacketWriter, m: ForumThreadsOut) = writeForumThreadsOut(w, m)

    private fun readForumThreadsOutList1Item(r: PacketReader): ForumThreadsOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vString3 = r.string()
        val vInt8 = r.int()
        val vByte1 = r.byte()
        val vInt9 = r.int()
        val vString4 = r.string()
        val vInt10 = r.int()
        return ForumThreadsOut.List1Item(vInt1, vInt2, vString1, vString2, vBoolean1, vBoolean2, vInt3, vInt4, vInt5, vInt6, vInt7, vString3, vInt8, vByte1, vInt9, vString4, vInt10)
    }

    private fun readForumThreadsOut(r: PacketReader): ForumThreadsOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vList1 = List(r.countInt(51)) { readForumThreadsOutList1Item(r) }
        return ForumThreadsOut(vInt1, vInt2, vList1)
    }

    private fun writeForumThreadsOutList1Item(w: PacketWriter, m: ForumThreadsOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.string(m.string2)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.string(m.string3)
        w.int(m.int8)
        w.byte(m.byte1)
        w.int(m.int9)
        w.string(m.string4)
        w.int(m.int10)
    }

    private fun writeForumThreadsOut(w: PacketWriter, m: ForumThreadsOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.countInt(m.list1.size)
        m.list1.forEach { writeForumThreadsOutList1Item(w, it) }
    }
}
