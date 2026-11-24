package zel.protocol.generated

data class PollAnswerIn(
    val pollId: Int,
    val questionId: Int,
    val answers: List<String>,
)

object PollAnswerInCodec : IncomingCodec<PollAnswerIn> {
    override val name = "PollAnswerIn"

    override fun read(r: PacketReader): PollAnswerIn = readPollAnswerIn(r)

    override fun write(w: PacketWriter, m: PollAnswerIn) = writePollAnswerIn(w, m)

    private fun readPollAnswerIn(r: PacketReader): PollAnswerIn {
        val vPollId = r.int()
        val vQuestionId = r.int()
        val vAnswers = List(r.countInt(2)) { r.string() }
        return PollAnswerIn(vPollId, vQuestionId, vAnswers)
    }

    private fun writePollAnswerIn(w: PacketWriter, m: PollAnswerIn) {
        w.int(m.pollId)
        w.int(m.questionId)
        w.countInt(m.answers.size)
        m.answers.forEach { w.string(it) }
    }
}
