package zel.protocol.generated

data class ChatReviewSessionVotingStatusOut(
    val list1: List<Int>,
)

object ChatReviewSessionVotingStatusOutCodec : OutgoingCodec<ChatReviewSessionVotingStatusOut> {
    override val name = "ChatReviewSessionVotingStatusOut"

    override fun read(r: PacketReader): ChatReviewSessionVotingStatusOut = readChatReviewSessionVotingStatusOut(r)

    override fun write(w: PacketWriter, m: ChatReviewSessionVotingStatusOut) = writeChatReviewSessionVotingStatusOut(w, m)

    private fun readChatReviewSessionVotingStatusOut(r: PacketReader): ChatReviewSessionVotingStatusOut {
        val vList1 = List(r.countInt(4)) { r.int() }
        return ChatReviewSessionVotingStatusOut(vList1)
    }

    private fun writeChatReviewSessionVotingStatusOut(w: PacketWriter, m: ChatReviewSessionVotingStatusOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }
}
