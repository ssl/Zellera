package zel.protocol.generated

data class WiredPlayerPermanentVariablesOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val string2: String,
    val rest: ByteArray,
)

object WiredPlayerPermanentVariablesOutCodec : OutgoingCodec<WiredPlayerPermanentVariablesOut> {
    override val name = "WiredPlayerPermanentVariablesOut"

    override fun read(r: PacketReader): WiredPlayerPermanentVariablesOut = readWiredPlayerPermanentVariablesOut(r)

    override fun write(w: PacketWriter, m: WiredPlayerPermanentVariablesOut) = writeWiredPlayerPermanentVariablesOut(w, m)

    private fun readWiredPlayerPermanentVariablesOut(r: PacketReader): WiredPlayerPermanentVariablesOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vRest = r.rest()
        return WiredPlayerPermanentVariablesOut(vInt1, vInt2, vString1, vString2, vRest)
    }

    private fun writeWiredPlayerPermanentVariablesOut(w: PacketWriter, m: WiredPlayerPermanentVariablesOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.string(m.string2)
        w.bytes(m.rest)
    }
}
