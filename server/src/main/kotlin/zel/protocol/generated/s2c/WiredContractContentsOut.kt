package zel.protocol.generated

data class WiredContractContentsOut(
    val int1: Int,
    val short1: Short,
    val boolean1: Boolean,
    val rest: ByteArray,
)

object WiredContractContentsOutCodec : OutgoingCodec<WiredContractContentsOut> {
    override val name = "WiredContractContentsOut"

    override fun read(r: PacketReader): WiredContractContentsOut = readWiredContractContentsOut(r)

    override fun write(w: PacketWriter, m: WiredContractContentsOut) = writeWiredContractContentsOut(w, m)

    private fun readWiredContractContentsOut(r: PacketReader): WiredContractContentsOut {
        val vInt1 = r.int()
        val vShort1 = r.short()
        val vBoolean1 = r.boolean()
        val vRest = r.rest()
        return WiredContractContentsOut(vInt1, vShort1, vBoolean1, vRest)
    }

    private fun writeWiredContractContentsOut(w: PacketWriter, m: WiredContractContentsOut) {
        w.int(m.int1)
        w.short(m.short1)
        w.boolean(m.boolean1)
        w.bytes(m.rest)
    }
}
