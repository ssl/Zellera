package zel.protocol.generated

data object CloseConnectionOut

object CloseConnectionOutCodec : OutgoingCodec<CloseConnectionOut> {
    override val name = "CloseConnectionOut"

    override fun read(r: PacketReader): CloseConnectionOut = CloseConnectionOut

    override fun write(w: PacketWriter, m: CloseConnectionOut) = writeCloseConnectionOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeCloseConnectionOut(w: PacketWriter, m: CloseConnectionOut) {
    }
}
