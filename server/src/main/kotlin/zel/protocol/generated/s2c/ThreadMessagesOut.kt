package zel.protocol.generated

data class ThreadMessagesOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
        val string1: String,
        val string2: String,
        val int4: Int,
        val string3: String,
        val byte1: Byte,
        val int5: Int,
        val string4: String,
        val int6: Int,
        val int7: Int,
    )
}

object ThreadMessagesOutCodec : OutgoingCodec<ThreadMessagesOut> {
    override val name = "ThreadMessagesOut"

    override fun read(r: PacketReader): ThreadMessagesOut = readThreadMessagesOut(r)

    override fun write(w: PacketWriter, m: ThreadMessagesOut) = writeThreadMessagesOut(w, m)

    private fun readThreadMessagesOutList1Item(r: PacketReader): ThreadMessagesOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt4 = r.int()
        val vString3 = r.string()
        val vByte1 = r.byte()
        val vInt5 = r.int()
        val vString4 = r.string()
        val vInt6 = r.int()
        val vInt7 = r.int()
        return ThreadMessagesOut.List1Item(vInt1, vInt2, vInt3, vString1, vString2, vInt4, vString3, vByte1, vInt5, vString4, vInt6, vInt7)
    }

    private fun readThreadMessagesOut(r: PacketReader): ThreadMessagesOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vList1 = List(r.countInt(37)) { readThreadMessagesOutList1Item(r) }
        return ThreadMessagesOut(vInt1, vInt2, vInt3, vList1)
    }

    private fun writeThreadMessagesOutList1Item(w: PacketWriter, m: ThreadMessagesOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int4)
        w.string(m.string3)
        w.byte(m.byte1)
        w.int(m.int5)
        w.string(m.string4)
        w.int(m.int6)
        w.int(m.int7)
    }

    private fun writeThreadMessagesOut(w: PacketWriter, m: ThreadMessagesOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.countInt(m.list1.size)
        m.list1.forEach { writeThreadMessagesOutList1Item(w, it) }
    }
}
