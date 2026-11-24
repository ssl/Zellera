package zel.protocol.generated

data class FaqClientFaqsOut(
    val list1: List<List1Item>,
    val list2: List<List2Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
    )

    data class List2Item(
        val int1: Int,
        val string1: String,
    )
}

object FaqClientFaqsOutCodec : OutgoingCodec<FaqClientFaqsOut> {
    override val name = "FaqClientFaqsOut"

    override fun read(r: PacketReader): FaqClientFaqsOut = readFaqClientFaqsOut(r)

    override fun write(w: PacketWriter, m: FaqClientFaqsOut) = writeFaqClientFaqsOut(w, m)

    private fun readFaqClientFaqsOutList1Item(r: PacketReader): FaqClientFaqsOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return FaqClientFaqsOut.List1Item(vInt1, vString1)
    }

    private fun readFaqClientFaqsOutList2Item(r: PacketReader): FaqClientFaqsOut.List2Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return FaqClientFaqsOut.List2Item(vInt1, vString1)
    }

    private fun readFaqClientFaqsOut(r: PacketReader): FaqClientFaqsOut {
        val vList1 = List(r.countInt(6)) { readFaqClientFaqsOutList1Item(r) }
        val vList2 = List(r.countInt(6)) { readFaqClientFaqsOutList2Item(r) }
        return FaqClientFaqsOut(vList1, vList2)
    }

    private fun writeFaqClientFaqsOutList1Item(w: PacketWriter, m: FaqClientFaqsOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeFaqClientFaqsOutList2Item(w: PacketWriter, m: FaqClientFaqsOut.List2Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeFaqClientFaqsOut(w: PacketWriter, m: FaqClientFaqsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeFaqClientFaqsOutList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { writeFaqClientFaqsOutList2Item(w, it) }
    }
}
