package zel.protocol.generated

data class QuestionFinishedOut(
    val int1: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val int1: Int,
    )
}

object QuestionFinishedOutCodec : OutgoingCodec<QuestionFinishedOut> {
    override val name = "QuestionFinishedOut"

    override fun read(r: PacketReader): QuestionFinishedOut = readQuestionFinishedOut(r)

    override fun write(w: PacketWriter, m: QuestionFinishedOut) = writeQuestionFinishedOut(w, m)

    private fun readQuestionFinishedOutList1Item(r: PacketReader): QuestionFinishedOut.List1Item {
        val vString1 = r.string()
        val vInt1 = r.int()
        return QuestionFinishedOut.List1Item(vString1, vInt1)
    }

    private fun readQuestionFinishedOut(r: PacketReader): QuestionFinishedOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(6)) { readQuestionFinishedOutList1Item(r) }
        return QuestionFinishedOut(vInt1, vList1)
    }

    private fun writeQuestionFinishedOutList1Item(w: PacketWriter, m: QuestionFinishedOut.List1Item) {
        w.string(m.string1)
        w.int(m.int1)
    }

    private fun writeQuestionFinishedOut(w: PacketWriter, m: QuestionFinishedOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeQuestionFinishedOutList1Item(w, it) }
    }
}
