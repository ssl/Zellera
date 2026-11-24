package zel.protocol.generated

data object CancelTypingIn

object CancelTypingInCodec : IncomingCodec<CancelTypingIn> {
    override val name = "CancelTypingIn"

    override fun read(r: PacketReader): CancelTypingIn = CancelTypingIn

    override fun write(w: PacketWriter, m: CancelTypingIn) = writeCancelTypingIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeCancelTypingIn(w: PacketWriter, m: CancelTypingIn) {
    }
}
