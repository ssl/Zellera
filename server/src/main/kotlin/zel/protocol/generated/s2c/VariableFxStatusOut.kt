package zel.protocol.generated

data class VariableFxStatusOut(
    val boolean1: Boolean,
    val rest: ByteArray,
)

object VariableFxStatusOutCodec : OutgoingCodec<VariableFxStatusOut> {
    override val name = "VariableFxStatusOut"

    override fun read(r: PacketReader): VariableFxStatusOut = readVariableFxStatusOut(r)

    override fun write(w: PacketWriter, m: VariableFxStatusOut) = writeVariableFxStatusOut(w, m)

    private fun readVariableFxStatusOut(r: PacketReader): VariableFxStatusOut {
        val vBoolean1 = r.boolean()
        val vRest = r.rest()
        return VariableFxStatusOut(vBoolean1, vRest)
    }

    private fun writeVariableFxStatusOut(w: PacketWriter, m: VariableFxStatusOut) {
        w.boolean(m.boolean1)
        w.bytes(m.rest)
    }
}
