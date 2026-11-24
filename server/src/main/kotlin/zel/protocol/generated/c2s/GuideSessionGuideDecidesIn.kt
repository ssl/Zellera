package zel.protocol.generated

data class GuideSessionGuideDecidesIn(
    val accepted: Boolean,
)

object GuideSessionGuideDecidesInCodec : IncomingCodec<GuideSessionGuideDecidesIn> {
    override val name = "GuideSessionGuideDecidesIn"

    override fun read(r: PacketReader): GuideSessionGuideDecidesIn = readGuideSessionGuideDecidesIn(r)

    override fun write(w: PacketWriter, m: GuideSessionGuideDecidesIn) = writeGuideSessionGuideDecidesIn(w, m)

    private fun readGuideSessionGuideDecidesIn(r: PacketReader): GuideSessionGuideDecidesIn {
        val vAccepted = r.boolean()
        return GuideSessionGuideDecidesIn(vAccepted)
    }

    private fun writeGuideSessionGuideDecidesIn(w: PacketWriter, m: GuideSessionGuideDecidesIn) {
        w.boolean(m.accepted)
    }
}
