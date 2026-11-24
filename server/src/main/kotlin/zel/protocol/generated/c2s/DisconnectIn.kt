package zel.protocol.generated

data object DisconnectIn

object DisconnectInCodec : IncomingCodec<DisconnectIn> {
    override val name = "DisconnectIn"

    override fun read(r: PacketReader): DisconnectIn = DisconnectIn

    override fun write(w: PacketWriter, m: DisconnectIn) = writeDisconnectIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeDisconnectIn(w: PacketWriter, m: DisconnectIn) {
    }
}
