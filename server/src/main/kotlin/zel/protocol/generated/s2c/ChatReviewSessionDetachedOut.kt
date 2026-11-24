package zel.protocol.generated

data object ChatReviewSessionDetachedOut

object ChatReviewSessionDetachedOutCodec : OutgoingCodec<ChatReviewSessionDetachedOut> {
    override val name = "ChatReviewSessionDetachedOut"

    override fun read(r: PacketReader): ChatReviewSessionDetachedOut = ChatReviewSessionDetachedOut

    override fun write(w: PacketWriter, m: ChatReviewSessionDetachedOut) = writeChatReviewSessionDetachedOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeChatReviewSessionDetachedOut(w: PacketWriter, m: ChatReviewSessionDetachedOut) {
    }
}
