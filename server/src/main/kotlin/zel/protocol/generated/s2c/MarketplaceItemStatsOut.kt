package zel.protocol.generated

data class MarketplaceItemStatsOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val list1: List<List1Item>,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val int7: Int,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
    )
}

object MarketplaceItemStatsOutCodec : OutgoingCodec<MarketplaceItemStatsOut> {
    override val name = "MarketplaceItemStatsOut"

    override fun read(r: PacketReader): MarketplaceItemStatsOut = readMarketplaceItemStatsOut(r)

    override fun write(w: PacketWriter, m: MarketplaceItemStatsOut) = writeMarketplaceItemStatsOut(w, m)

    private fun readMarketplaceItemStatsOutList1Item(r: PacketReader): MarketplaceItemStatsOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return MarketplaceItemStatsOut.List1Item(vInt1, vInt2, vInt3)
    }

    private fun readMarketplaceItemStatsOut(r: PacketReader): MarketplaceItemStatsOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vList1 = List(r.countInt(12)) { readMarketplaceItemStatsOutList1Item(r) }
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        return MarketplaceItemStatsOut(vInt1, vInt2, vInt3, vList1, vInt4, vInt5, vInt6, vInt7)
    }

    private fun writeMarketplaceItemStatsOutList1Item(w: PacketWriter, m: MarketplaceItemStatsOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }

    private fun writeMarketplaceItemStatsOut(w: PacketWriter, m: MarketplaceItemStatsOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.countInt(m.list1.size)
        m.list1.forEach { writeMarketplaceItemStatsOutList1Item(w, it) }
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
    }
}
