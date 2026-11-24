package zel.protocol.generated

data class WiredSetPlayerPermanentVariableResultOut(
    val boolean1: Boolean,
)

object WiredSetPlayerPermanentVariableResultOutCodec : OutgoingCodec<WiredSetPlayerPermanentVariableResultOut> {
    override val name = "WiredSetPlayerPermanentVariableResultOut"

    override fun read(r: PacketReader): WiredSetPlayerPermanentVariableResultOut = readWiredSetPlayerPermanentVariableResultOut(r)

    override fun write(w: PacketWriter, m: WiredSetPlayerPermanentVariableResultOut) = writeWiredSetPlayerPermanentVariableResultOut(w, m)

    private fun readWiredSetPlayerPermanentVariableResultOut(r: PacketReader): WiredSetPlayerPermanentVariableResultOut {
        val vBoolean1 = r.boolean()
        return WiredSetPlayerPermanentVariableResultOut(vBoolean1)
    }

    private fun writeWiredSetPlayerPermanentVariableResultOut(w: PacketWriter, m: WiredSetPlayerPermanentVariableResultOut) {
        w.boolean(m.boolean1)
    }
}
