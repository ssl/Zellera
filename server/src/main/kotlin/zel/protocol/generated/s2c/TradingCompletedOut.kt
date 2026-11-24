package zel.protocol.generated

data object TradingCompletedOut

object TradingCompletedOutCodec : OutgoingCodec<TradingCompletedOut> {
    override val name = "TradingCompletedOut"

    override fun read(r: PacketReader): TradingCompletedOut = TradingCompletedOut

    override fun write(w: PacketWriter, m: TradingCompletedOut) = writeTradingCompletedOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeTradingCompletedOut(w: PacketWriter, m: TradingCompletedOut) {
    }
}
