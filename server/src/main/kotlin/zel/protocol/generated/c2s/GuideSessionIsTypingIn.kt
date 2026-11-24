package zel.protocol.generated

data class GuideSessionIsTypingIn(
    val typing: Boolean,
)

object GuideSessionIsTypingInCodec : IncomingCodec<GuideSessionIsTypingIn> {
    override val name = "GuideSessionIsTypingIn"

    override fun read(r: PacketReader): GuideSessionIsTypingIn = readGuideSessionIsTypingIn(r)

    override fun write(w: PacketWriter, m: GuideSessionIsTypingIn) = writeGuideSessionIsTypingIn(w, m)

    private fun readGuideSessionIsTypingIn(r: PacketReader): GuideSessionIsTypingIn {
        val vTyping = r.boolean()
        return GuideSessionIsTypingIn(vTyping)
    }

    private fun writeGuideSessionIsTypingIn(w: PacketWriter, m: GuideSessionIsTypingIn) {
        w.boolean(m.typing)
    }
}
