package zel.protocol.generated

data class NoSuchFlatOut(
    val int1: Int,
)

object NoSuchFlatOutCodec : OutgoingCodec<NoSuchFlatOut> {
    override val name = "NoSuchFlatOut"

    override fun read(r: PacketReader): NoSuchFlatOut = readNoSuchFlatOut(r)

    override fun write(w: PacketWriter, m: NoSuchFlatOut) = writeNoSuchFlatOut(w, m)

    private fun readNoSuchFlatOut(r: PacketReader): NoSuchFlatOut {
        val vInt1 = r.int()
        return NoSuchFlatOut(vInt1)
    }

    private fun writeNoSuchFlatOut(w: PacketWriter, m: NoSuchFlatOut) {
        w.int(m.int1)
    }
}
