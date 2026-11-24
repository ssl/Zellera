package zel.protocol.generated

data object ChatReviewGuideDetachedIn

object ChatReviewGuideDetachedInCodec : IncomingCodec<ChatReviewGuideDetachedIn> {
    override val name = "ChatReviewGuideDetachedIn"

    override fun read(r: PacketReader): ChatReviewGuideDetachedIn = ChatReviewGuideDetachedIn

    override fun write(w: PacketWriter, m: ChatReviewGuideDetachedIn) = writeChatReviewGuideDetachedIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeChatReviewGuideDetachedIn(w: PacketWriter, m: ChatReviewGuideDetachedIn) {
    }
}
