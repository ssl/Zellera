package zel.protocol.generated

data class ChatReviewSessionCreateIn(
    val userId: Int,
    val roomId: Int,
)

object ChatReviewSessionCreateInCodec : IncomingCodec<ChatReviewSessionCreateIn> {
    override val name = "ChatReviewSessionCreateIn"

    override fun read(r: PacketReader): ChatReviewSessionCreateIn = readChatReviewSessionCreateIn(r)

    override fun write(w: PacketWriter, m: ChatReviewSessionCreateIn) = writeChatReviewSessionCreateIn(w, m)

    private fun readChatReviewSessionCreateIn(r: PacketReader): ChatReviewSessionCreateIn {
        val vUserId = r.int()
        val vRoomId = r.int()
        return ChatReviewSessionCreateIn(vUserId, vRoomId)
    }

    private fun writeChatReviewSessionCreateIn(w: PacketWriter, m: ChatReviewSessionCreateIn) {
        w.int(m.userId)
        w.int(m.roomId)
    }
}
