package zel.protocol.generated

data object TradingNotOpenOut

object TradingNotOpenOutCodec : OutgoingCodec<TradingNotOpenOut> {
    override val name = "TradingNotOpenOut"

    override fun read(r: PacketReader): TradingNotOpenOut = TradingNotOpenOut

    override fun write(w: PacketWriter, m: TradingNotOpenOut) = writeTradingNotOpenOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeTradingNotOpenOut(w: PacketWriter, m: TradingNotOpenOut) {
    }
}
