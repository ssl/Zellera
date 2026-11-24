package zel.protocol.generated

data class GetQuizQuestionsIn(
    val quizCode: String,
)

object GetQuizQuestionsInCodec : IncomingCodec<GetQuizQuestionsIn> {
    override val name = "GetQuizQuestionsIn"

    override fun read(r: PacketReader): GetQuizQuestionsIn = readGetQuizQuestionsIn(r)

    override fun write(w: PacketWriter, m: GetQuizQuestionsIn) = writeGetQuizQuestionsIn(w, m)

    private fun readGetQuizQuestionsIn(r: PacketReader): GetQuizQuestionsIn {
        val vQuizCode = r.string()
        return GetQuizQuestionsIn(vQuizCode)
    }

    private fun writeGetQuizQuestionsIn(w: PacketWriter, m: GetQuizQuestionsIn) {
        w.string(m.quizCode)
    }
}
