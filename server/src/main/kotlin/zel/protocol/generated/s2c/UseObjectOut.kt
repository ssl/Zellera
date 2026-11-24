package zel.protocol.generated

data class UseObjectOut(
    val int1: Int,
    val int2: Int,
)

object UseObjectOutCodec : OutgoingCodec<UseObjectOut> {
    override val name = "UseObjectOut"

    override fun read(r: PacketReader): UseObjectOut = readUseObjectOut(r)

    override fun write(w: PacketWriter, m: UseObjectOut) = writeUseObjectOut(w, m)

    private fun readUseObjectOut(r: PacketReader): UseObjectOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return UseObjectOut(vInt1, vInt2)
    }

    private fun writeUseObjectOut(w: PacketWriter, m: UseObjectOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
