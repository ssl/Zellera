package zel.protocol.generated

data class PresentOpenedOut(
    val string1: String,
    val int1: Int,
    val string2: String,
    val int2: Int,
    val string3: String,
    val boolean1: Boolean,
    val string4: String,
)

object PresentOpenedOutCodec : OutgoingCodec<PresentOpenedOut> {
    override val name = "PresentOpenedOut"

    override fun read(r: PacketReader): PresentOpenedOut = readPresentOpenedOut(r)

    override fun write(w: PacketWriter, m: PresentOpenedOut) = writePresentOpenedOut(w, m)

    private fun readPresentOpenedOut(r: PacketReader): PresentOpenedOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vString2 = r.string()
        val vInt2 = r.int()
        val vString3 = r.string()
        val vBoolean1 = r.boolean()
        val vString4 = r.string()
        return PresentOpenedOut(vString1, vInt1, vString2, vInt2, vString3, vBoolean1, vString4)
    }

    private fun writePresentOpenedOut(w: PacketWriter, m: PresentOpenedOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.string(m.string2)
        w.int(m.int2)
        w.string(m.string3)
        w.boolean(m.boolean1)
        w.string(m.string4)
    }
}
