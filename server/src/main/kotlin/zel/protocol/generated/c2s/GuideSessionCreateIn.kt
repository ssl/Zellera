package zel.protocol.generated

data class GuideSessionCreateIn(
    val int1: Int,
    val message: String,
)

object GuideSessionCreateInCodec : IncomingCodec<GuideSessionCreateIn> {
    override val name = "GuideSessionCreateIn"

    override fun read(r: PacketReader): GuideSessionCreateIn = readGuideSessionCreateIn(r)

    override fun write(w: PacketWriter, m: GuideSessionCreateIn) = writeGuideSessionCreateIn(w, m)

    private fun readGuideSessionCreateIn(r: PacketReader): GuideSessionCreateIn {
        val vInt1 = r.int()
        val vMessage = r.string()
        return GuideSessionCreateIn(vInt1, vMessage)
    }

    private fun writeGuideSessionCreateIn(w: PacketWriter, m: GuideSessionCreateIn) {
        w.int(m.int1)
        w.string(m.message)
    }
}
