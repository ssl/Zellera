package zel.protocol.generated

data class WiredClickPlayerResponseOut(
    val int1: Int,
    val boolean1: Boolean,
)

object WiredClickPlayerResponseOutCodec : OutgoingCodec<WiredClickPlayerResponseOut> {
    override val name = "WiredClickPlayerResponseOut"

    override fun read(r: PacketReader): WiredClickPlayerResponseOut = readWiredClickPlayerResponseOut(r)

    override fun write(w: PacketWriter, m: WiredClickPlayerResponseOut) = writeWiredClickPlayerResponseOut(w, m)

    private fun readWiredClickPlayerResponseOut(r: PacketReader): WiredClickPlayerResponseOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return WiredClickPlayerResponseOut(vInt1, vBoolean1)
    }

    private fun writeWiredClickPlayerResponseOut(w: PacketWriter, m: WiredClickPlayerResponseOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
    }
}
