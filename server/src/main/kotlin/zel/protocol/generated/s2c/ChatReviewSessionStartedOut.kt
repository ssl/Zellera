package zel.protocol.generated

data class ChatReviewSessionStartedOut(
    val int1: Int,
    val string1: String,
)

object ChatReviewSessionStartedOutCodec : OutgoingCodec<ChatReviewSessionStartedOut> {
    override val name = "ChatReviewSessionStartedOut"

    override fun read(r: PacketReader): ChatReviewSessionStartedOut = readChatReviewSessionStartedOut(r)

    override fun write(w: PacketWriter, m: ChatReviewSessionStartedOut) = writeChatReviewSessionStartedOut(w, m)

    private fun readChatReviewSessionStartedOut(r: PacketReader): ChatReviewSessionStartedOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        return ChatReviewSessionStartedOut(vInt1, vString1)
    }

    private fun writeChatReviewSessionStartedOut(w: PacketWriter, m: ChatReviewSessionStartedOut) {
        w.int(m.int1)
        w.string(m.string1)
    }
}
