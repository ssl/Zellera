package zel.protocol.generated

data object TradingOtherNotAllowedOut

object TradingOtherNotAllowedOutCodec : OutgoingCodec<TradingOtherNotAllowedOut> {
    override val name = "TradingOtherNotAllowedOut"

    override fun read(r: PacketReader): TradingOtherNotAllowedOut = TradingOtherNotAllowedOut

    override fun write(w: PacketWriter, m: TradingOtherNotAllowedOut) = writeTradingOtherNotAllowedOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeTradingOtherNotAllowedOut(w: PacketWriter, m: TradingOtherNotAllowedOut) {
    }
}
