package zel.protocol.generated

data object ClientWindowFocusedIn

object ClientWindowFocusedInCodec : IncomingCodec<ClientWindowFocusedIn> {
    override val name = "ClientWindowFocusedIn"

    override fun read(r: PacketReader): ClientWindowFocusedIn = ClientWindowFocusedIn

    override fun write(w: PacketWriter, m: ClientWindowFocusedIn) = writeClientWindowFocusedIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeClientWindowFocusedIn(w: PacketWriter, m: ClientWindowFocusedIn) {
    }
}
