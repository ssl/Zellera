package zel.protocol.generated

data object CloseTradingIn

object CloseTradingInCodec : IncomingCodec<CloseTradingIn> {
    override val name = "CloseTradingIn"

    override fun read(r: PacketReader): CloseTradingIn = CloseTradingIn

    override fun write(w: PacketWriter, m: CloseTradingIn) = writeCloseTradingIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeCloseTradingIn(w: PacketWriter, m: CloseTradingIn) {
    }
}
