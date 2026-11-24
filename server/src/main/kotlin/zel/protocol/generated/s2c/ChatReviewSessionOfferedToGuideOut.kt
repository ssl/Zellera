package zel.protocol.generated

data class ChatReviewSessionOfferedToGuideOut(
    val int1: Int,
)

object ChatReviewSessionOfferedToGuideOutCodec : OutgoingCodec<ChatReviewSessionOfferedToGuideOut> {
    override val name = "ChatReviewSessionOfferedToGuideOut"

    override fun read(r: PacketReader): ChatReviewSessionOfferedToGuideOut = readChatReviewSessionOfferedToGuideOut(r)

    override fun write(w: PacketWriter, m: ChatReviewSessionOfferedToGuideOut) = writeChatReviewSessionOfferedToGuideOut(w, m)

    private fun readChatReviewSessionOfferedToGuideOut(r: PacketReader): ChatReviewSessionOfferedToGuideOut {
        val vInt1 = r.int()
        return ChatReviewSessionOfferedToGuideOut(vInt1)
    }

    private fun writeChatReviewSessionOfferedToGuideOut(w: PacketWriter, m: ChatReviewSessionOfferedToGuideOut) {
        w.int(m.int1)
    }
}
