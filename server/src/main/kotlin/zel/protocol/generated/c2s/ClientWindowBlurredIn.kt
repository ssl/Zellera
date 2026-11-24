package zel.protocol.generated

data object ClientWindowBlurredIn

object ClientWindowBlurredInCodec : IncomingCodec<ClientWindowBlurredIn> {
    override val name = "ClientWindowBlurredIn"

    override fun read(r: PacketReader): ClientWindowBlurredIn = ClientWindowBlurredIn

    override fun write(w: PacketWriter, m: ClientWindowBlurredIn) = writeClientWindowBlurredIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeClientWindowBlurredIn(w: PacketWriter, m: ClientWindowBlurredIn) {
    }
}
