package zel.protocol.generated

data class WiredLinkOperationResultOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val boolean1: Boolean,
    val rest: ByteArray,
)

object WiredLinkOperationResultOutCodec : OutgoingCodec<WiredLinkOperationResultOut> {
    override val name = "WiredLinkOperationResultOut"

    override fun read(r: PacketReader): WiredLinkOperationResultOut = readWiredLinkOperationResultOut(r)

    override fun write(w: PacketWriter, m: WiredLinkOperationResultOut) = writeWiredLinkOperationResultOut(w, m)

    private fun readWiredLinkOperationResultOut(r: PacketReader): WiredLinkOperationResultOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vBoolean1 = r.boolean()
        val vRest = r.rest()
        return WiredLinkOperationResultOut(vInt1, vInt2, vInt3, vInt4, vBoolean1, vRest)
    }

    private fun writeWiredLinkOperationResultOut(w: PacketWriter, m: WiredLinkOperationResultOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.boolean(m.boolean1)
        w.bytes(m.rest)
    }
}
