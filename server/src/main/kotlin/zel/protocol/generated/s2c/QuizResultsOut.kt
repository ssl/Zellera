package zel.protocol.generated

data class QuizResultsOut(
    val string1: String,
    val list1: List<Int>,
)

object QuizResultsOutCodec : OutgoingCodec<QuizResultsOut> {
    override val name = "QuizResultsOut"

    override fun read(r: PacketReader): QuizResultsOut = readQuizResultsOut(r)

    override fun write(w: PacketWriter, m: QuizResultsOut) = writeQuizResultsOut(w, m)

    private fun readQuizResultsOut(r: PacketReader): QuizResultsOut {
        val vString1 = r.string()
        val vList1 = List(r.countInt(4)) { r.int() }
        return QuizResultsOut(vString1, vList1)
    }

    private fun writeQuizResultsOut(w: PacketWriter, m: QuizResultsOut) {
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }
}
