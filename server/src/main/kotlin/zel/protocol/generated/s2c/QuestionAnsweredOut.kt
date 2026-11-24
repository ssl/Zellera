package zel.protocol.generated

data class QuestionAnsweredOut(
    val int1: Int,
    val string1: String,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val int1: Int,
    )
}

object QuestionAnsweredOutCodec : OutgoingCodec<QuestionAnsweredOut> {
    override val name = "QuestionAnsweredOut"

    override fun read(r: PacketReader): QuestionAnsweredOut = readQuestionAnsweredOut(r)

    override fun write(w: PacketWriter, m: QuestionAnsweredOut) = writeQuestionAnsweredOut(w, m)

    private fun readQuestionAnsweredOutList1Item(r: PacketReader): QuestionAnsweredOut.List1Item {
        val vString1 = r.string()
        val vInt1 = r.int()
        return QuestionAnsweredOut.List1Item(vString1, vInt1)
    }

    private fun readQuestionAnsweredOut(r: PacketReader): QuestionAnsweredOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vList1 = List(r.countInt(6)) { readQuestionAnsweredOutList1Item(r) }
        return QuestionAnsweredOut(vInt1, vString1, vList1)
    }

    private fun writeQuestionAnsweredOutList1Item(w: PacketWriter, m: QuestionAnsweredOut.List1Item) {
        w.string(m.string1)
        w.int(m.int1)
    }

    private fun writeQuestionAnsweredOut(w: PacketWriter, m: QuestionAnsweredOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeQuestionAnsweredOutList1Item(w, it) }
    }
}
