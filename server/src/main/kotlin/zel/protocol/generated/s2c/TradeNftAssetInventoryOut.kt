package zel.protocol.generated

data class TradeNftAssetInventoryOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val long1: Long,
        val short1: Short,
        val string1: String,
        val int1: Int,
        val string2: String,
        val list1: List<Int>,
        val string3: String,
        val string4: String,
    )
}

object TradeNftAssetInventoryOutCodec : OutgoingCodec<TradeNftAssetInventoryOut> {
    override val name = "TradeNftAssetInventoryOut"

    override fun read(r: PacketReader): TradeNftAssetInventoryOut = readTradeNftAssetInventoryOut(r)

    override fun write(w: PacketWriter, m: TradeNftAssetInventoryOut) = writeTradeNftAssetInventoryOut(w, m)

    private fun readTradeNftAssetInventoryOutList1Item(r: PacketReader): TradeNftAssetInventoryOut.List1Item {
        val vLong1 = r.long()
        val vShort1 = r.short()
        val vString1 = r.string()
        val vInt1 = r.int()
        val vString2 = r.string()
        val vList1 = List(r.countInt(4)) { r.int() }
        val vString3 = r.string()
        val vString4 = r.string()
        return TradeNftAssetInventoryOut.List1Item(vLong1, vShort1, vString1, vInt1, vString2, vList1, vString3, vString4)
    }

    private fun readTradeNftAssetInventoryOut(r: PacketReader): TradeNftAssetInventoryOut {
        val vList1 = List(r.countInt(26)) { readTradeNftAssetInventoryOutList1Item(r) }
        return TradeNftAssetInventoryOut(vList1)
    }

    private fun writeTradeNftAssetInventoryOutList1Item(w: PacketWriter, m: TradeNftAssetInventoryOut.List1Item) {
        w.long(m.long1)
        w.short(m.short1)
        w.string(m.string1)
        w.int(m.int1)
        w.string(m.string2)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.string(m.string3)
        w.string(m.string4)
    }

    private fun writeTradeNftAssetInventoryOut(w: PacketWriter, m: TradeNftAssetInventoryOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeTradeNftAssetInventoryOutList1Item(w, it) }
    }
}
