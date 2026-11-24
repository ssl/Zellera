package zel.protocol.generated

data class IssueInfoOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val int7: Int,
    val int8: Int,
    val string1: String,
    val int9: Int,
    val string2: String,
    val int10: Int,
    val string3: String,
    val string4: String,
    val int11: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val int1: Int,
        val int2: Int,
    )
}

object IssueInfoOutCodec : OutgoingCodec<IssueInfoOut> {
    override val name = "IssueInfoOut"

    override fun read(r: PacketReader): IssueInfoOut = readIssueInfoOut(r)

    override fun write(w: PacketWriter, m: IssueInfoOut) = writeIssueInfoOut(w, m)

    private fun readIssueInfoOutList1Item(r: PacketReader): IssueInfoOut.List1Item {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        return IssueInfoOut.List1Item(vString1, vInt1, vInt2)
    }

    private fun readIssueInfoOut(r: PacketReader): IssueInfoOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vString1 = r.string()
        val vInt9 = r.int()
        val vString2 = r.string()
        val vInt10 = r.int()
        val vString3 = r.string()
        val vString4 = r.string()
        val vInt11 = r.int()
        val vList1 = List(r.countInt(10)) { readIssueInfoOutList1Item(r) }
        return IssueInfoOut(vInt1, vInt2, vInt3, vInt4, vInt5, vInt6, vInt7, vInt8, vString1, vInt9, vString2, vInt10, vString3, vString4, vInt11, vList1)
    }

    private fun writeIssueInfoOutList1Item(w: PacketWriter, m: IssueInfoOut.List1Item) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writeIssueInfoOut(w: PacketWriter, m: IssueInfoOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.string(m.string1)
        w.int(m.int9)
        w.string(m.string2)
        w.int(m.int10)
        w.string(m.string3)
        w.string(m.string4)
        w.int(m.int11)
        w.countInt(m.list1.size)
        m.list1.forEach { writeIssueInfoOutList1Item(w, it) }
    }
}
