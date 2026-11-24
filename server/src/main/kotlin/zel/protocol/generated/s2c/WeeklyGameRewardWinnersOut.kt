package zel.protocol.generated

data class WeeklyGameRewardWinnersOut(
    val int1: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val string2: String,
        val string3: String,
        val int1: Int,
        val int2: Int,
    )
}

object WeeklyGameRewardWinnersOutCodec : OutgoingCodec<WeeklyGameRewardWinnersOut> {
    override val name = "WeeklyGameRewardWinnersOut"

    override fun read(r: PacketReader): WeeklyGameRewardWinnersOut = readWeeklyGameRewardWinnersOut(r)

    override fun write(w: PacketWriter, m: WeeklyGameRewardWinnersOut) = writeWeeklyGameRewardWinnersOut(w, m)

    private fun readWeeklyGameRewardWinnersOutList1Item(r: PacketReader): WeeklyGameRewardWinnersOut.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        return WeeklyGameRewardWinnersOut.List1Item(vString1, vString2, vString3, vInt1, vInt2)
    }

    private fun readWeeklyGameRewardWinnersOut(r: PacketReader): WeeklyGameRewardWinnersOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(14)) { readWeeklyGameRewardWinnersOutList1Item(r) }
        return WeeklyGameRewardWinnersOut(vInt1, vList1)
    }

    private fun writeWeeklyGameRewardWinnersOutList1Item(w: PacketWriter, m: WeeklyGameRewardWinnersOut.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writeWeeklyGameRewardWinnersOut(w: PacketWriter, m: WeeklyGameRewardWinnersOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeWeeklyGameRewardWinnersOutList1Item(w, it) }
    }
}
