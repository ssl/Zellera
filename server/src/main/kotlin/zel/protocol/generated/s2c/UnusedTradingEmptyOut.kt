package zel.protocol.generated

data object UnusedTradingEmptyOut

object UnusedTradingEmptyOutCodec : OutgoingCodec<UnusedTradingEmptyOut> {
    override val name = "UnusedTradingEmptyOut"

    override fun read(r: PacketReader): UnusedTradingEmptyOut = UnusedTradingEmptyOut

    override fun write(w: PacketWriter, m: UnusedTradingEmptyOut) = writeUnusedTradingEmptyOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeUnusedTradingEmptyOut(w: PacketWriter, m: UnusedTradingEmptyOut) {
    }
}
