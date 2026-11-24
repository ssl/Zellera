package zel.protocol.generated

data object MessengerInitIn

object MessengerInitInCodec : IncomingCodec<MessengerInitIn> {
    override val name = "MessengerInitIn"

    override fun read(r: PacketReader): MessengerInitIn = MessengerInitIn

    override fun write(w: PacketWriter, m: MessengerInitIn) = writeMessengerInitIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeMessengerInitIn(w: PacketWriter, m: MessengerInitIn) {
    }
}
