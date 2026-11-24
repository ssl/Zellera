package zel.protocol.generated

data class IgnoreResultOut(
    val int1: Int,
    val int2: Int,
)

object IgnoreResultOutCodec : OutgoingCodec<IgnoreResultOut> {
    override val name = "IgnoreResultOut"

    override fun read(r: PacketReader): IgnoreResultOut = readIgnoreResultOut(r)

    override fun write(w: PacketWriter, m: IgnoreResultOut) = writeIgnoreResultOut(w, m)

    private fun readIgnoreResultOut(r: PacketReader): IgnoreResultOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return IgnoreResultOut(vInt1, vInt2)
    }

    private fun writeIgnoreResultOut(w: PacketWriter, m: IgnoreResultOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
