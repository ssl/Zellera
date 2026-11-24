package zel.protocol.generated

data class GuideSessionMessageOut(
    val string1: String,
    val int1: Int,
)

object GuideSessionMessageOutCodec : OutgoingCodec<GuideSessionMessageOut> {
    override val name = "GuideSessionMessageOut"

    override fun read(r: PacketReader): GuideSessionMessageOut = readGuideSessionMessageOut(r)

    override fun write(w: PacketWriter, m: GuideSessionMessageOut) = writeGuideSessionMessageOut(w, m)

    private fun readGuideSessionMessageOut(r: PacketReader): GuideSessionMessageOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        return GuideSessionMessageOut(vString1, vInt1)
    }

    private fun writeGuideSessionMessageOut(w: PacketWriter, m: GuideSessionMessageOut) {
        w.string(m.string1)
        w.int(m.int1)
    }
}
