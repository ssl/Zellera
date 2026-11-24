package zel.protocol.generated

data class OpenOut(
    val int1: Int,
)

object OpenOutCodec : OutgoingCodec<OpenOut> {
    override val name = "OpenOut"

    override fun read(r: PacketReader): OpenOut = readOpenOut(r)

    override fun write(w: PacketWriter, m: OpenOut) = writeOpenOut(w, m)

    private fun readOpenOut(r: PacketReader): OpenOut {
        val vInt1 = r.int()
        return OpenOut(vInt1)
    }

    private fun writeOpenOut(w: PacketWriter, m: OpenOut) {
        w.int(m.int1)
    }
}
