package zel.protocol.generated

data class FaqSearchResultsOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
    )
}

object FaqSearchResultsOutCodec : OutgoingCodec<FaqSearchResultsOut> {
    override val name = "FaqSearchResultsOut"

    override fun read(r: PacketReader): FaqSearchResultsOut = readFaqSearchResultsOut(r)

    override fun write(w: PacketWriter, m: FaqSearchResultsOut) = writeFaqSearchResultsOut(w, m)

    private fun readFaqSearchResultsOutList1Item(r: PacketReader): FaqSearchResultsOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return FaqSearchResultsOut.List1Item(vInt1, vString1)
    }

    private fun readFaqSearchResultsOut(r: PacketReader): FaqSearchResultsOut {
        val vList1 = List(r.countInt(6)) { readFaqSearchResultsOutList1Item(r) }
        return FaqSearchResultsOut(vList1)
    }

    private fun writeFaqSearchResultsOutList1Item(w: PacketWriter, m: FaqSearchResultsOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeFaqSearchResultsOut(w: PacketWriter, m: FaqSearchResultsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeFaqSearchResultsOutList1Item(w, it) }
    }
}
