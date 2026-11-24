package zel.protocol.generated

data class DisconnectReasonOut(
    val trailing1: Int?,
)

object DisconnectReasonOutCodec : OutgoingCodec<DisconnectReasonOut> {
    override val name = "DisconnectReasonOut"

    override fun read(r: PacketReader): DisconnectReasonOut = readDisconnectReasonOut(r)

    override fun write(w: PacketWriter, m: DisconnectReasonOut) = writeDisconnectReasonOut(w, m)

    private fun readDisconnectReasonOut(r: PacketReader): DisconnectReasonOut {
        val vTrailing1 = if (r.remaining > 0) r.int() else null
        return DisconnectReasonOut(vTrailing1)
    }

    private fun writeDisconnectReasonOut(w: PacketWriter, m: DisconnectReasonOut) {
        m.trailing1?.let { w.int(it) }
    }
}
