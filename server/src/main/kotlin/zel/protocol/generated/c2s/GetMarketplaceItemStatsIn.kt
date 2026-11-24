package zel.protocol.generated

data class GetMarketplaceItemStatsIn(
    val int1: Int,
    val int2: Int,
    val trailing1: String?,
)

object GetMarketplaceItemStatsInCodec : IncomingCodec<GetMarketplaceItemStatsIn> {
    override val name = "GetMarketplaceItemStatsIn"

    override fun read(r: PacketReader): GetMarketplaceItemStatsIn = readGetMarketplaceItemStatsIn(r)

    override fun write(w: PacketWriter, m: GetMarketplaceItemStatsIn) = writeGetMarketplaceItemStatsIn(w, m)

    private fun readGetMarketplaceItemStatsIn(r: PacketReader): GetMarketplaceItemStatsIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vTrailing1 = if (r.remaining > 0) r.string() else null
        return GetMarketplaceItemStatsIn(vInt1, vInt2, vTrailing1)
    }

    private fun writeGetMarketplaceItemStatsIn(w: PacketWriter, m: GetMarketplaceItemStatsIn) {
        w.int(m.int1)
        w.int(m.int2)
        m.trailing1?.let { w.string(it) }
    }
}
