package zel.protocol.generated

data class ClearMarketplaceOwnHistoryIn(
    val state: Int,
)

object ClearMarketplaceOwnHistoryInCodec : IncomingCodec<ClearMarketplaceOwnHistoryIn> {
    override val name = "ClearMarketplaceOwnHistoryIn"

    override fun read(r: PacketReader): ClearMarketplaceOwnHistoryIn = readClearMarketplaceOwnHistoryIn(r)

    override fun write(w: PacketWriter, m: ClearMarketplaceOwnHistoryIn) = writeClearMarketplaceOwnHistoryIn(w, m)

    private fun readClearMarketplaceOwnHistoryIn(r: PacketReader): ClearMarketplaceOwnHistoryIn {
        val vState = r.int()
        return ClearMarketplaceOwnHistoryIn(vState)
    }

    private fun writeClearMarketplaceOwnHistoryIn(w: PacketWriter, m: ClearMarketplaceOwnHistoryIn) {
        w.int(m.state)
    }
}
