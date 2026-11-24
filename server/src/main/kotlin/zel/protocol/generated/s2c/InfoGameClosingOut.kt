package zel.protocol.generated

data class InfoGameClosingOut(
    val int1: Int,
)

object InfoGameClosingOutCodec : OutgoingCodec<InfoGameClosingOut> {
    override val name = "InfoGameClosingOut"

    override fun read(r: PacketReader): InfoGameClosingOut = readInfoGameClosingOut(r)

    override fun write(w: PacketWriter, m: InfoGameClosingOut) = writeInfoGameClosingOut(w, m)

    private fun readInfoGameClosingOut(r: PacketReader): InfoGameClosingOut {
        val vInt1 = r.int()
        return InfoGameClosingOut(vInt1)
    }

    private fun writeInfoGameClosingOut(w: PacketWriter, m: InfoGameClosingOut) {
        w.int(m.int1)
    }
}
