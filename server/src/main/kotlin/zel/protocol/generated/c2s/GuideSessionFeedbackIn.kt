package zel.protocol.generated

data class GuideSessionFeedbackIn(
    val recommend: Boolean,
)

object GuideSessionFeedbackInCodec : IncomingCodec<GuideSessionFeedbackIn> {
    override val name = "GuideSessionFeedbackIn"

    override fun read(r: PacketReader): GuideSessionFeedbackIn = readGuideSessionFeedbackIn(r)

    override fun write(w: PacketWriter, m: GuideSessionFeedbackIn) = writeGuideSessionFeedbackIn(w, m)

    private fun readGuideSessionFeedbackIn(r: PacketReader): GuideSessionFeedbackIn {
        val vRecommend = r.boolean()
        return GuideSessionFeedbackIn(vRecommend)
    }

    private fun writeGuideSessionFeedbackIn(w: PacketWriter, m: GuideSessionFeedbackIn) {
        w.boolean(m.recommend)
    }
}
