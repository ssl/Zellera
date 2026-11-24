package zel.protocol.generated

data class CantConnectOut(
    val int1: Int,
    val rest: ByteArray,
)

object CantConnectOutCodec : OutgoingCodec<CantConnectOut> {
    override val name = "CantConnectOut"

    override fun read(r: PacketReader): CantConnectOut = readCantConnectOut(r)

    override fun write(w: PacketWriter, m: CantConnectOut) = writeCantConnectOut(w, m)

    private fun readCantConnectOut(r: PacketReader): CantConnectOut {
        val vInt1 = r.int()
        val vRest = r.rest()
        return CantConnectOut(vInt1, vRest)
    }

    private fun writeCantConnectOut(w: PacketWriter, m: CantConnectOut) {
        w.int(m.int1)
        w.bytes(m.rest)
    }
}
