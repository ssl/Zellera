package zel.protocol.generated

data class IssuePickFailedOut(
    val list1: List<List1Item>,
    val boolean1: Boolean,
    val int1: Int,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val string1: String,
    )
}

object IssuePickFailedOutCodec : OutgoingCodec<IssuePickFailedOut> {
    override val name = "IssuePickFailedOut"

    override fun read(r: PacketReader): IssuePickFailedOut = readIssuePickFailedOut(r)

    override fun write(w: PacketWriter, m: IssuePickFailedOut) = writeIssuePickFailedOut(w, m)

    private fun readIssuePickFailedOutList1Item(r: PacketReader): IssuePickFailedOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        return IssuePickFailedOut.List1Item(vInt1, vInt2, vString1)
    }

    private fun readIssuePickFailedOut(r: PacketReader): IssuePickFailedOut {
        val vList1 = List(r.countInt(10)) { readIssuePickFailedOutList1Item(r) }
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        return IssuePickFailedOut(vList1, vBoolean1, vInt1)
    }

    private fun writeIssuePickFailedOutList1Item(w: PacketWriter, m: IssuePickFailedOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
    }

    private fun writeIssuePickFailedOut(w: PacketWriter, m: IssuePickFailedOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeIssuePickFailedOutList1Item(w, it) }
        w.boolean(m.boolean1)
        w.int(m.int1)
    }
}
