package zel.protocol.generated

data class ChatReviewGuideDecidesOnOfferIn(
    val boolean1: Boolean,
)

object ChatReviewGuideDecidesOnOfferInCodec : IncomingCodec<ChatReviewGuideDecidesOnOfferIn> {
    override val name = "ChatReviewGuideDecidesOnOfferIn"

    override fun read(r: PacketReader): ChatReviewGuideDecidesOnOfferIn = readChatReviewGuideDecidesOnOfferIn(r)

    override fun write(w: PacketWriter, m: ChatReviewGuideDecidesOnOfferIn) = writeChatReviewGuideDecidesOnOfferIn(w, m)

    private fun readChatReviewGuideDecidesOnOfferIn(r: PacketReader): ChatReviewGuideDecidesOnOfferIn {
        val vBoolean1 = r.boolean()
        return ChatReviewGuideDecidesOnOfferIn(vBoolean1)
    }

    private fun writeChatReviewGuideDecidesOnOfferIn(w: PacketWriter, m: ChatReviewGuideDecidesOnOfferIn) {
        w.boolean(m.boolean1)
    }
}
