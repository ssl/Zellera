package zel.protocol.generated

data class WiredTradeInitiateOut(
    val int1: Int,
    val string1: String,
    val string2: String,
    val rest: ByteArray,
)

object WiredTradeInitiateOutCodec : OutgoingCodec<WiredTradeInitiateOut> {
    override val name = "WiredTradeInitiateOut"

    override fun read(r: PacketReader): WiredTradeInitiateOut = readWiredTradeInitiateOut(r)

    override fun write(w: PacketWriter, m: WiredTradeInitiateOut) = writeWiredTradeInitiateOut(w, m)

    private fun readWiredTradeInitiateOut(r: PacketReader): WiredTradeInitiateOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vRest = r.rest()
        return WiredTradeInitiateOut(vInt1, vString1, vString2, vRest)
    }

    private fun writeWiredTradeInitiateOut(w: PacketWriter, m: WiredTradeInitiateOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.bytes(m.rest)
    }
}
