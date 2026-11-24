package zel.protocol.generated

data class PostQuizAnswersIn(
    val quizCode: String,
    val answers: List<Int>,
)

object PostQuizAnswersInCodec : IncomingCodec<PostQuizAnswersIn> {
    override val name = "PostQuizAnswersIn"

    override fun read(r: PacketReader): PostQuizAnswersIn = readPostQuizAnswersIn(r)

    override fun write(w: PacketWriter, m: PostQuizAnswersIn) = writePostQuizAnswersIn(w, m)

    private fun readPostQuizAnswersIn(r: PacketReader): PostQuizAnswersIn {
        val vQuizCode = r.string()
        val vAnswers = List(r.countInt(4)) { r.int() }
        return PostQuizAnswersIn(vQuizCode, vAnswers)
    }

    private fun writePostQuizAnswersIn(w: PacketWriter, m: PostQuizAnswersIn) {
        w.string(m.quizCode)
        w.countInt(m.answers.size)
        m.answers.forEach { w.int(it) }
    }
}
