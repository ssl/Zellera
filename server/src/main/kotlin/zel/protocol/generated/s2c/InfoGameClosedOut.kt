package zel.protocol.generated

data class InfoGameClosedOut(
    val int1: Int,
    val int2: Int,
    val boolean1: Boolean,
)

object InfoGameClosedOutCodec : OutgoingCodec<InfoGameClosedOut> {
    override val name = "InfoGameClosedOut"

    override fun read(r: PacketReader): InfoGameClosedOut = readInfoGameClosedOut(r)

    override fun write(w: PacketWriter, m: InfoGameClosedOut) = writeInfoGameClosedOut(w, m)

    private fun readInfoGameClosedOut(r: PacketReader): InfoGameClosedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        return InfoGameClosedOut(vInt1, vInt2, vBoolean1)
    }

    private fun writeInfoGameClosedOut(w: PacketWriter, m: InfoGameClosedOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.boolean(m.boolean1)
    }
}
