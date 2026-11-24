package zel.protocol.generated

data object TradingConfirmationOut

object TradingConfirmationOutCodec : OutgoingCodec<TradingConfirmationOut> {
    override val name = "TradingConfirmationOut"

    override fun read(r: PacketReader): TradingConfirmationOut = TradingConfirmationOut

    override fun write(w: PacketWriter, m: TradingConfirmationOut) = writeTradingConfirmationOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeTradingConfirmationOut(w: PacketWriter, m: TradingConfirmationOut) {
    }
}
