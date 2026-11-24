package zel.protocol.generated

data class GuideSessionPartnerIsTypingOut(
    val boolean1: Boolean,
)

object GuideSessionPartnerIsTypingOutCodec : OutgoingCodec<GuideSessionPartnerIsTypingOut> {
    override val name = "GuideSessionPartnerIsTypingOut"

    override fun read(r: PacketReader): GuideSessionPartnerIsTypingOut = readGuideSessionPartnerIsTypingOut(r)

    override fun write(w: PacketWriter, m: GuideSessionPartnerIsTypingOut) = writeGuideSessionPartnerIsTypingOut(w, m)

    private fun readGuideSessionPartnerIsTypingOut(r: PacketReader): GuideSessionPartnerIsTypingOut {
        val vBoolean1 = r.boolean()
        return GuideSessionPartnerIsTypingOut(vBoolean1)
    }

    private fun writeGuideSessionPartnerIsTypingOut(w: PacketWriter, m: GuideSessionPartnerIsTypingOut) {
        w.boolean(m.boolean1)
    }
}
