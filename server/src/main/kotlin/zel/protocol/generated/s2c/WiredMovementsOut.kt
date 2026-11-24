package zel.protocol.generated

data class WiredMovementsOut(
    val rest: ByteArray,
)

object WiredMovementsOutCodec : OutgoingCodec<WiredMovementsOut> {
    override val name = "WiredMovementsOut"

    override fun read(r: PacketReader): WiredMovementsOut = readWiredMovementsOut(r)

    override fun write(w: PacketWriter, m: WiredMovementsOut) = writeWiredMovementsOut(w, m)

    private fun readWiredMovementsOut(r: PacketReader): WiredMovementsOut {
        val vRest = r.rest()
        return WiredMovementsOut(vRest)
    }

    private fun writeWiredMovementsOut(w: PacketWriter, m: WiredMovementsOut) {
        w.bytes(m.rest)
    }
}
