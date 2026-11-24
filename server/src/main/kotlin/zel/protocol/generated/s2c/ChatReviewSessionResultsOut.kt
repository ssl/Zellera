package zel.protocol.generated

data class ChatReviewSessionResultsOut(
    val int1: Int,
    val int2: Int,
    val list1: List<Int>,
)

object ChatReviewSessionResultsOutCodec : OutgoingCodec<ChatReviewSessionResultsOut> {
    override val name = "ChatReviewSessionResultsOut"

    override fun read(r: PacketReader): ChatReviewSessionResultsOut = readChatReviewSessionResultsOut(r)

    override fun write(w: PacketWriter, m: ChatReviewSessionResultsOut) = writeChatReviewSessionResultsOut(w, m)

    private fun readChatReviewSessionResultsOut(r: PacketReader): ChatReviewSessionResultsOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vList1 = List(r.countInt(4)) { r.int() }
        return ChatReviewSessionResultsOut(vInt1, vInt2, vList1)
    }

    private fun writeChatReviewSessionResultsOut(w: PacketWriter, m: ChatReviewSessionResultsOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }
}
