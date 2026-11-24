package zel.protocol.generated

data class GuideSessionEndedOut(
    val int1: Int,
)

object GuideSessionEndedOutCodec : OutgoingCodec<GuideSessionEndedOut> {
    override val name = "GuideSessionEndedOut"

    override fun read(r: PacketReader): GuideSessionEndedOut = readGuideSessionEndedOut(r)

    override fun write(w: PacketWriter, m: GuideSessionEndedOut) = writeGuideSessionEndedOut(w, m)

    private fun readGuideSessionEndedOut(r: PacketReader): GuideSessionEndedOut {
        val vInt1 = r.int()
        return GuideSessionEndedOut(vInt1)
    }

    private fun writeGuideSessionEndedOut(w: PacketWriter, m: GuideSessionEndedOut) {
        w.int(m.int1)
    }
}
