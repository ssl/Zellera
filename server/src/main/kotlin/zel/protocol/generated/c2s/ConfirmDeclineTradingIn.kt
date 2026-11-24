package zel.protocol.generated

data object ConfirmDeclineTradingIn

object ConfirmDeclineTradingInCodec : IncomingCodec<ConfirmDeclineTradingIn> {
    override val name = "ConfirmDeclineTradingIn"

    override fun read(r: PacketReader): ConfirmDeclineTradingIn = ConfirmDeclineTradingIn

    override fun write(w: PacketWriter, m: ConfirmDeclineTradingIn) = writeConfirmDeclineTradingIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeConfirmDeclineTradingIn(w: PacketWriter, m: ConfirmDeclineTradingIn) {
    }
}
