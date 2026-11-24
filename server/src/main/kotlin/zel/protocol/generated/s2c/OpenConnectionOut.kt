package zel.protocol.generated

data class OpenConnectionOut(
    val int1: Int,
)

object OpenConnectionOutCodec : OutgoingCodec<OpenConnectionOut> {
    override val name = "OpenConnectionOut"

    override fun read(r: PacketReader): OpenConnectionOut = readOpenConnectionOut(r)

    override fun write(w: PacketWriter, m: OpenConnectionOut) = writeOpenConnectionOut(w, m)

    private fun readOpenConnectionOut(r: PacketReader): OpenConnectionOut {
        val vInt1 = r.int()
        return OpenConnectionOut(vInt1)
    }

    private fun writeOpenConnectionOut(w: PacketWriter, m: OpenConnectionOut) {
        w.int(m.int1)
    }
}
