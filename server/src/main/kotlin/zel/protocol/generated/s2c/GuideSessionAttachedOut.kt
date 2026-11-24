package zel.protocol.generated

data class GuideSessionAttachedOut(
    val boolean1: Boolean,
    val int1: Int,
    val string1: String,
    val int2: Int,
)

object GuideSessionAttachedOutCodec : OutgoingCodec<GuideSessionAttachedOut> {
    override val name = "GuideSessionAttachedOut"

    override fun read(r: PacketReader): GuideSessionAttachedOut = readGuideSessionAttachedOut(r)

    override fun write(w: PacketWriter, m: GuideSessionAttachedOut) = writeGuideSessionAttachedOut(w, m)

    private fun readGuideSessionAttachedOut(r: PacketReader): GuideSessionAttachedOut {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        return GuideSessionAttachedOut(vBoolean1, vInt1, vString1, vInt2)
    }

    private fun writeGuideSessionAttachedOut(w: PacketWriter, m: GuideSessionAttachedOut) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
    }
}
