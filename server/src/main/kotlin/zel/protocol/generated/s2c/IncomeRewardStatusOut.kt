package zel.protocol.generated

data class IncomeRewardStatusOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val byte1: Byte,
        val byte2: Byte,
        val int1: Int,
        val string1: String,
    )
}

object IncomeRewardStatusOutCodec : OutgoingCodec<IncomeRewardStatusOut> {
    override val name = "IncomeRewardStatusOut"

    override fun read(r: PacketReader): IncomeRewardStatusOut = readIncomeRewardStatusOut(r)

    override fun write(w: PacketWriter, m: IncomeRewardStatusOut) = writeIncomeRewardStatusOut(w, m)

    private fun readIncomeRewardStatusOutList1Item(r: PacketReader): IncomeRewardStatusOut.List1Item {
        val vByte1 = r.byte()
        val vByte2 = r.byte()
        val vInt1 = r.int()
        val vString1 = r.string()
        return IncomeRewardStatusOut.List1Item(vByte1, vByte2, vInt1, vString1)
    }

    private fun readIncomeRewardStatusOut(r: PacketReader): IncomeRewardStatusOut {
        val vList1 = List(r.countInt(8)) { readIncomeRewardStatusOutList1Item(r) }
        return IncomeRewardStatusOut(vList1)
    }

    private fun writeIncomeRewardStatusOutList1Item(w: PacketWriter, m: IncomeRewardStatusOut.List1Item) {
        w.byte(m.byte1)
        w.byte(m.byte2)
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeIncomeRewardStatusOut(w: PacketWriter, m: IncomeRewardStatusOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeIncomeRewardStatusOutList1Item(w, it) }
    }
}
