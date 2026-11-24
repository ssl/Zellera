package zel.protocol.generated

data class GuideSessionErrorOut(
    val int1: Int,
)

object GuideSessionErrorOutCodec : OutgoingCodec<GuideSessionErrorOut> {
    override val name = "GuideSessionErrorOut"

    override fun read(r: PacketReader): GuideSessionErrorOut = readGuideSessionErrorOut(r)

    override fun write(w: PacketWriter, m: GuideSessionErrorOut) = writeGuideSessionErrorOut(w, m)

    private fun readGuideSessionErrorOut(r: PacketReader): GuideSessionErrorOut {
        val vInt1 = r.int()
        return GuideSessionErrorOut(vInt1)
    }

    private fun writeGuideSessionErrorOut(w: PacketWriter, m: GuideSessionErrorOut) {
        w.int(m.int1)
    }
}
