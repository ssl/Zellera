package zel.protocol.generated

data object ConfirmAcceptTradingIn

object ConfirmAcceptTradingInCodec : IncomingCodec<ConfirmAcceptTradingIn> {
    override val name = "ConfirmAcceptTradingIn"

    override fun read(r: PacketReader): ConfirmAcceptTradingIn = ConfirmAcceptTradingIn

    override fun write(w: PacketWriter, m: ConfirmAcceptTradingIn) = writeConfirmAcceptTradingIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeConfirmAcceptTradingIn(w: PacketWriter, m: ConfirmAcceptTradingIn) {
    }
}
