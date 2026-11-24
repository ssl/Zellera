package zel.protocol.generated

data class ChatReviewGuideVoteIn(
    val voteType: Int,
)

object ChatReviewGuideVoteInCodec : IncomingCodec<ChatReviewGuideVoteIn> {
    override val name = "ChatReviewGuideVoteIn"

    override fun read(r: PacketReader): ChatReviewGuideVoteIn = readChatReviewGuideVoteIn(r)

    override fun write(w: PacketWriter, m: ChatReviewGuideVoteIn) = writeChatReviewGuideVoteIn(w, m)

    private fun readChatReviewGuideVoteIn(r: PacketReader): ChatReviewGuideVoteIn {
        val vVoteType = r.int()
        return ChatReviewGuideVoteIn(vVoteType)
    }

    private fun writeChatReviewGuideVoteIn(w: PacketWriter, m: ChatReviewGuideVoteIn) {
        w.int(m.voteType)
    }
}
