package zel.protocol.generated

data class TradeNftAssetsOut(
    val list1: List<List1Item>,
    val list2: List<List2Item>,
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

    data class List2Item(
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

object TradeNftAssetsOutCodec : OutgoingCodec<TradeNftAssetsOut> {
    override val name = "TradeNftAssetsOut"

    override fun read(r: PacketReader): TradeNftAssetsOut = readTradeNftAssetsOut(r)

    override fun write(w: PacketWriter, m: TradeNftAssetsOut) = writeTradeNftAssetsOut(w, m)

    private fun readTradeNftAssetsOutList1Item(r: PacketReader): TradeNftAssetsOut.List1Item {
        val vLong1 = r.long()
        val vShort1 = r.short()
        val vString1 = r.string()
        val vInt1 = r.int()
        val vString2 = r.string()
        val vList1 = List(r.countInt(4)) { r.int() }
        val vString3 = r.string()
        val vString4 = r.string()
        return TradeNftAssetsOut.List1Item(vLong1, vShort1, vString1, vInt1, vString2, vList1, vString3, vString4)
    }

    private fun readTradeNftAssetsOutList2Item(r: PacketReader): TradeNftAssetsOut.List2Item {
        val vLong1 = r.long()
        val vShort1 = r.short()
        val vString1 = r.string()
        val vInt1 = r.int()
        val vString2 = r.string()
        val vList1 = List(r.countInt(4)) { r.int() }
        val vString3 = r.string()
        val vString4 = r.string()
        return TradeNftAssetsOut.List2Item(vLong1, vShort1, vString1, vInt1, vString2, vList1, vString3, vString4)
    }

    private fun readTradeNftAssetsOut(r: PacketReader): TradeNftAssetsOut {
        val vList1 = List(r.countInt(26)) { readTradeNftAssetsOutList1Item(r) }
        val vList2 = List(r.countInt(26)) { readTradeNftAssetsOutList2Item(r) }
        return TradeNftAssetsOut(vList1, vList2)
    }

    private fun writeTradeNftAssetsOutList1Item(w: PacketWriter, m: TradeNftAssetsOut.List1Item) {
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

    private fun writeTradeNftAssetsOutList2Item(w: PacketWriter, m: TradeNftAssetsOut.List2Item) {
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

    private fun writeTradeNftAssetsOut(w: PacketWriter, m: TradeNftAssetsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeTradeNftAssetsOutList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { writeTradeNftAssetsOutList2Item(w, it) }
    }
}
