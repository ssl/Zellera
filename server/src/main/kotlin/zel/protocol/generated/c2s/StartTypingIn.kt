package zel.protocol.generated

data object StartTypingIn

object StartTypingInCodec : IncomingCodec<StartTypingIn> {
    override val name = "StartTypingIn"

    override fun read(r: PacketReader): StartTypingIn = StartTypingIn

    override fun write(w: PacketWriter, m: StartTypingIn) = writeStartTypingIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeStartTypingIn(w: PacketWriter, m: StartTypingIn) {
    }
}
