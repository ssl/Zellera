package zel.protocol.generated

data class GuideSessionMessageIn(
    val string1: String,
)

object GuideSessionMessageInCodec : IncomingCodec<GuideSessionMessageIn> {
    override val name = "GuideSessionMessageIn"

    override fun read(r: PacketReader): GuideSessionMessageIn = readGuideSessionMessageIn(r)

    override fun write(w: PacketWriter, m: GuideSessionMessageIn) = writeGuideSessionMessageIn(w, m)

    private fun readGuideSessionMessageIn(r: PacketReader): GuideSessionMessageIn {
        val vString1 = r.string()
        return GuideSessionMessageIn(vString1)
    }

    private fun writeGuideSessionMessageIn(w: PacketWriter, m: GuideSessionMessageIn) {
        w.string(m.string1)
    }
}
