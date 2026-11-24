package zel.protocol.generated

data class InstantMessageErrorOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
)

object InstantMessageErrorOutCodec : OutgoingCodec<InstantMessageErrorOut> {
    override val name = "InstantMessageErrorOut"

    override fun read(r: PacketReader): InstantMessageErrorOut = readInstantMessageErrorOut(r)

    override fun write(w: PacketWriter, m: InstantMessageErrorOut) = writeInstantMessageErrorOut(w, m)

    private fun readInstantMessageErrorOut(r: PacketReader): InstantMessageErrorOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        return InstantMessageErrorOut(vInt1, vInt2, vString1)
    }

    private fun writeInstantMessageErrorOut(w: PacketWriter, m: InstantMessageErrorOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
    }
}
