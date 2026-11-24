package zel.protocol.generated

data class WiredPlayerVariablesListOut(
    val string1: String,
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val rest: ByteArray,
)

object WiredPlayerVariablesListOutCodec : OutgoingCodec<WiredPlayerVariablesListOut> {
    override val name = "WiredPlayerVariablesListOut"

    override fun read(r: PacketReader): WiredPlayerVariablesListOut = readWiredPlayerVariablesListOut(r)

    override fun write(w: PacketWriter, m: WiredPlayerVariablesListOut) = writeWiredPlayerVariablesListOut(w, m)

    private fun readWiredPlayerVariablesListOut(r: PacketReader): WiredPlayerVariablesListOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vRest = r.rest()
        return WiredPlayerVariablesListOut(vString1, vInt1, vInt2, vInt3, vRest)
    }

    private fun writeWiredPlayerVariablesListOut(w: PacketWriter, m: WiredPlayerVariablesListOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.bytes(m.rest)
    }
}
