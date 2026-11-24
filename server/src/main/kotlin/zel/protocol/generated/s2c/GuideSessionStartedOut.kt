package zel.protocol.generated

data class GuideSessionStartedOut(
    val int1: Int,
    val string1: String,
    val string2: String,
    val int2: Int,
    val string3: String,
    val string4: String,
)

object GuideSessionStartedOutCodec : OutgoingCodec<GuideSessionStartedOut> {
    override val name = "GuideSessionStartedOut"

    override fun read(r: PacketReader): GuideSessionStartedOut = readGuideSessionStartedOut(r)

    override fun write(w: PacketWriter, m: GuideSessionStartedOut) = writeGuideSessionStartedOut(w, m)

    private fun readGuideSessionStartedOut(r: PacketReader): GuideSessionStartedOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt2 = r.int()
        val vString3 = r.string()
        val vString4 = r.string()
        return GuideSessionStartedOut(vInt1, vString1, vString2, vInt2, vString3, vString4)
    }

    private fun writeGuideSessionStartedOut(w: PacketWriter, m: GuideSessionStartedOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int2)
        w.string(m.string3)
        w.string(m.string4)
    }
}
