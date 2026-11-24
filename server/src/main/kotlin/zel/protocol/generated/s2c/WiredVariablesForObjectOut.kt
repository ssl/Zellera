package zel.protocol.generated

data class WiredVariablesForObjectOut(
    val int1: Int,
    val rest: ByteArray,
)

object WiredVariablesForObjectOutCodec : OutgoingCodec<WiredVariablesForObjectOut> {
    override val name = "WiredVariablesForObjectOut"

    override fun read(r: PacketReader): WiredVariablesForObjectOut = readWiredVariablesForObjectOut(r)

    override fun write(w: PacketWriter, m: WiredVariablesForObjectOut) = writeWiredVariablesForObjectOut(w, m)

    private fun readWiredVariablesForObjectOut(r: PacketReader): WiredVariablesForObjectOut {
        val vInt1 = r.int()
        val vRest = r.rest()
        return WiredVariablesForObjectOut(vInt1, vRest)
    }

    private fun writeWiredVariablesForObjectOut(w: PacketWriter, m: WiredVariablesForObjectOut) {
        w.int(m.int1)
        w.bytes(m.rest)
    }
}
