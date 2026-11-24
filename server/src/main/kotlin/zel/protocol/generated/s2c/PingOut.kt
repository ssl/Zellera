package zel.protocol.generated

data object PingOut

object PingOutCodec : OutgoingCodec<PingOut> {
    override val name = "PingOut"

    override fun read(r: PacketReader): PingOut = PingOut

    override fun write(w: PacketWriter, m: PingOut) = writePingOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writePingOut(w: PacketWriter, m: PingOut) {
    }
}
