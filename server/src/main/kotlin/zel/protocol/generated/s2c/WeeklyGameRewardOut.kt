package zel.protocol.generated

data class WeeklyGameRewardOut(
    val int1: Int,
    val list1: List<List1Item>,
    val int2: Int,
    val boolean1: Boolean,
) {
    data class List1Item(
        val string1: String,
        val branch1: Branch1,
    ) {
        sealed interface Branch1 {
            data class Then(
                val string1: String,
            ) : Branch1
            data class Else(
                val int1: Int,
                val string1: String,
                val int2: Int,
                val boolean1: Boolean,
                val branch1: Branch1,
            ) : Branch1 {
                sealed interface Branch1 {
                    data class Then(
                        val int1: Int,
                        val int2: Int,
                    ) : Branch1
                    data object Else : Branch1
                }
            }
        }
    }
}

object WeeklyGameRewardOutCodec : OutgoingCodec<WeeklyGameRewardOut> {
    override val name = "WeeklyGameRewardOut"

    override fun read(r: PacketReader): WeeklyGameRewardOut = readWeeklyGameRewardOut(r)

    override fun write(w: PacketWriter, m: WeeklyGameRewardOut) = writeWeeklyGameRewardOut(w, m)

    private fun readWeeklyGameRewardOutList1ItemBranch1Then(r: PacketReader): WeeklyGameRewardOut.List1Item.Branch1.Then {
        val vString1 = r.string()
        return WeeklyGameRewardOut.List1Item.Branch1.Then(vString1)
    }

    private fun readWeeklyGameRewardOutList1ItemBranch1ElseBranch1Then(r: PacketReader): WeeklyGameRewardOut.List1Item.Branch1.Else.Branch1.Then {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return WeeklyGameRewardOut.List1Item.Branch1.Else.Branch1.Then(vInt1, vInt2)
    }

    private fun readWeeklyGameRewardOutList1ItemBranch1Else(r: PacketReader): WeeklyGameRewardOut.List1Item.Branch1.Else {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        val vBranch1 = if (vBoolean1 == true) readWeeklyGameRewardOutList1ItemBranch1ElseBranch1Then(r) else WeeklyGameRewardOut.List1Item.Branch1.Else.Branch1.Else
        return WeeklyGameRewardOut.List1Item.Branch1.Else(vInt1, vString1, vInt2, vBoolean1, vBranch1)
    }

    private fun readWeeklyGameRewardOutList1Item(r: PacketReader): WeeklyGameRewardOut.List1Item {
        val vString1 = r.string()
        val vBranch1 = if (vString1 == "b") readWeeklyGameRewardOutList1ItemBranch1Then(r) else readWeeklyGameRewardOutList1ItemBranch1Else(r)
        return WeeklyGameRewardOut.List1Item(vString1, vBranch1)
    }

    private fun readWeeklyGameRewardOut(r: PacketReader): WeeklyGameRewardOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(4)) { readWeeklyGameRewardOutList1Item(r) }
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        return WeeklyGameRewardOut(vInt1, vList1, vInt2, vBoolean1)
    }

    private fun writeWeeklyGameRewardOutList1ItemBranch1Then(w: PacketWriter, m: WeeklyGameRewardOut.List1Item.Branch1.Then) {
        w.string(m.string1)
    }

    private fun writeWeeklyGameRewardOutList1ItemBranch1ElseBranch1Then(w: PacketWriter, m: WeeklyGameRewardOut.List1Item.Branch1.Else.Branch1.Then) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writeWeeklyGameRewardOutList1ItemBranch1Else(w: PacketWriter, m: WeeklyGameRewardOut.List1Item.Branch1.Else) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.boolean(m.boolean1)
        when (val b = m.branch1) {
            is WeeklyGameRewardOut.List1Item.Branch1.Else.Branch1.Then -> writeWeeklyGameRewardOutList1ItemBranch1ElseBranch1Then(w, b)
            is WeeklyGameRewardOut.List1Item.Branch1.Else.Branch1.Else -> {}
        }
    }

    private fun writeWeeklyGameRewardOutList1Item(w: PacketWriter, m: WeeklyGameRewardOut.List1Item) {
        w.string(m.string1)
        when (val b = m.branch1) {
            is WeeklyGameRewardOut.List1Item.Branch1.Then -> writeWeeklyGameRewardOutList1ItemBranch1Then(w, b)
            is WeeklyGameRewardOut.List1Item.Branch1.Else -> writeWeeklyGameRewardOutList1ItemBranch1Else(w, b)
        }
    }

    private fun writeWeeklyGameRewardOut(w: PacketWriter, m: WeeklyGameRewardOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeWeeklyGameRewardOutList1Item(w, it) }
        w.int(m.int2)
        w.boolean(m.boolean1)
    }
}
