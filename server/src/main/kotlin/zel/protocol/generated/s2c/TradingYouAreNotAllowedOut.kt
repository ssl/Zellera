package zel.protocol.generated

data object TradingYouAreNotAllowedOut

object TradingYouAreNotAllowedOutCodec : OutgoingCodec<TradingYouAreNotAllowedOut> {
    override val name = "TradingYouAreNotAllowedOut"

    override fun read(r: PacketReader): TradingYouAreNotAllowedOut = TradingYouAreNotAllowedOut

    override fun write(w: PacketWriter, m: TradingYouAreNotAllowedOut) = writeTradingYouAreNotAllowedOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeTradingYouAreNotAllowedOut(w: PacketWriter, m: TradingYouAreNotAllowedOut) {
    }
}
