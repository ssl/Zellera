package zel.protocol.generated

data class QuizDataOut(
    val string1: String,
    val list1: List<Int>,
)

object QuizDataOutCodec : OutgoingCodec<QuizDataOut> {
    override val name = "QuizDataOut"

    override fun read(r: PacketReader): QuizDataOut = readQuizDataOut(r)

    override fun write(w: PacketWriter, m: QuizDataOut) = writeQuizDataOut(w, m)

    private fun readQuizDataOut(r: PacketReader): QuizDataOut {
        val vString1 = r.string()
        val vList1 = List(r.countInt(4)) { r.int() }
        return QuizDataOut(vString1, vList1)
    }

    private fun writeQuizDataOut(w: PacketWriter, m: QuizDataOut) {
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }
}
