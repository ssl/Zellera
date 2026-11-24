package zel.protocol.generated

data class FaqCategoryOut(
    val int1: Int,
    val string1: String,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
    )
}

object FaqCategoryOutCodec : OutgoingCodec<FaqCategoryOut> {
    override val name = "FaqCategoryOut"

    override fun read(r: PacketReader): FaqCategoryOut = readFaqCategoryOut(r)

    override fun write(w: PacketWriter, m: FaqCategoryOut) = writeFaqCategoryOut(w, m)

    private fun readFaqCategoryOutList1Item(r: PacketReader): FaqCategoryOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return FaqCategoryOut.List1Item(vInt1, vString1)
    }

    private fun readFaqCategoryOut(r: PacketReader): FaqCategoryOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vList1 = List(r.countInt(6)) { readFaqCategoryOutList1Item(r) }
        return FaqCategoryOut(vInt1, vString1, vList1)
    }

    private fun writeFaqCategoryOutList1Item(w: PacketWriter, m: FaqCategoryOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeFaqCategoryOut(w: PacketWriter, m: FaqCategoryOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeFaqCategoryOutList1Item(w, it) }
    }
}
