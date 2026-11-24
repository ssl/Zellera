package zel.protocol.generated

data class FaqCategoriesOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val int2: Int,
    )
}

object FaqCategoriesOutCodec : OutgoingCodec<FaqCategoriesOut> {
    override val name = "FaqCategoriesOut"

    override fun read(r: PacketReader): FaqCategoriesOut = readFaqCategoriesOut(r)

    override fun write(w: PacketWriter, m: FaqCategoriesOut) = writeFaqCategoriesOut(w, m)

    private fun readFaqCategoriesOutList1Item(r: PacketReader): FaqCategoriesOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        return FaqCategoriesOut.List1Item(vInt1, vString1, vInt2)
    }

    private fun readFaqCategoriesOut(r: PacketReader): FaqCategoriesOut {
        val vList1 = List(r.countInt(10)) { readFaqCategoriesOutList1Item(r) }
        return FaqCategoriesOut(vList1)
    }

    private fun writeFaqCategoriesOutList1Item(w: PacketWriter, m: FaqCategoriesOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
    }

    private fun writeFaqCategoriesOut(w: PacketWriter, m: FaqCategoriesOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeFaqCategoriesOutList1Item(w, it) }
    }
}
