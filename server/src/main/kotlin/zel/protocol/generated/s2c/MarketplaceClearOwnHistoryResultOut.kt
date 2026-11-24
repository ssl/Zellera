package zel.protocol.generated

data class MarketplaceClearOwnHistoryResultOut(
    val boolean1: Boolean,
)

object MarketplaceClearOwnHistoryResultOutCodec : OutgoingCodec<MarketplaceClearOwnHistoryResultOut> {
    override val name = "MarketplaceClearOwnHistoryResultOut"

    override fun read(r: PacketReader): MarketplaceClearOwnHistoryResultOut = readMarketplaceClearOwnHistoryResultOut(r)

    override fun write(w: PacketWriter, m: MarketplaceClearOwnHistoryResultOut) = writeMarketplaceClearOwnHistoryResultOut(w, m)

    private fun readMarketplaceClearOwnHistoryResultOut(r: PacketReader): MarketplaceClearOwnHistoryResultOut {
        val vBoolean1 = r.boolean()
        return MarketplaceClearOwnHistoryResultOut(vBoolean1)
    }

    private fun writeMarketplaceClearOwnHistoryResultOut(w: PacketWriter, m: MarketplaceClearOwnHistoryResultOut) {
        w.boolean(m.boolean1)
    }
}
