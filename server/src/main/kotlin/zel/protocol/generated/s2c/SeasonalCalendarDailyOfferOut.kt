package zel.protocol.generated

data class SeasonalCalendarDailyOfferOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val boolean1: Boolean,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val boolean2: Boolean,
    val list1: List<List1Item>,
    val int7: Int,
    val boolean3: Boolean,
    val boolean4: Boolean,
    val string2: String,
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

object SeasonalCalendarDailyOfferOutCodec : OutgoingCodec<SeasonalCalendarDailyOfferOut> {
    override val name = "SeasonalCalendarDailyOfferOut"

    override fun read(r: PacketReader): SeasonalCalendarDailyOfferOut = readSeasonalCalendarDailyOfferOut(r)

    override fun write(w: PacketWriter, m: SeasonalCalendarDailyOfferOut) = writeSeasonalCalendarDailyOfferOut(w, m)

    private fun readSeasonalCalendarDailyOfferOutList1ItemBranch1Then(r: PacketReader): SeasonalCalendarDailyOfferOut.List1Item.Branch1.Then {
        val vString1 = r.string()
        return SeasonalCalendarDailyOfferOut.List1Item.Branch1.Then(vString1)
    }

    private fun readSeasonalCalendarDailyOfferOutList1ItemBranch1ElseBranch1Then(r: PacketReader): SeasonalCalendarDailyOfferOut.List1Item.Branch1.Else.Branch1.Then {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return SeasonalCalendarDailyOfferOut.List1Item.Branch1.Else.Branch1.Then(vInt1, vInt2)
    }

    private fun readSeasonalCalendarDailyOfferOutList1ItemBranch1Else(r: PacketReader): SeasonalCalendarDailyOfferOut.List1Item.Branch1.Else {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        val vBranch1 = if (vBoolean1 == true) readSeasonalCalendarDailyOfferOutList1ItemBranch1ElseBranch1Then(r) else SeasonalCalendarDailyOfferOut.List1Item.Branch1.Else.Branch1.Else
        return SeasonalCalendarDailyOfferOut.List1Item.Branch1.Else(vInt1, vString1, vInt2, vBoolean1, vBranch1)
    }

    private fun readSeasonalCalendarDailyOfferOutList1Item(r: PacketReader): SeasonalCalendarDailyOfferOut.List1Item {
        val vString1 = r.string()
        val vBranch1 = if (vString1 == "b") readSeasonalCalendarDailyOfferOutList1ItemBranch1Then(r) else readSeasonalCalendarDailyOfferOutList1ItemBranch1Else(r)
        return SeasonalCalendarDailyOfferOut.List1Item(vString1, vBranch1)
    }

    private fun readSeasonalCalendarDailyOfferOut(r: PacketReader): SeasonalCalendarDailyOfferOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vBoolean1 = r.boolean()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vBoolean2 = r.boolean()
        val vList1 = List(r.countInt(4)) { readSeasonalCalendarDailyOfferOutList1Item(r) }
        val vInt7 = r.int()
        val vBoolean3 = r.boolean()
        val vBoolean4 = r.boolean()
        val vString2 = r.string()
        return SeasonalCalendarDailyOfferOut(vInt1, vInt2, vString1, vBoolean1, vInt3, vInt4, vInt5, vInt6, vBoolean2, vList1, vInt7, vBoolean3, vBoolean4, vString2)
    }

    private fun writeSeasonalCalendarDailyOfferOutList1ItemBranch1Then(w: PacketWriter, m: SeasonalCalendarDailyOfferOut.List1Item.Branch1.Then) {
        w.string(m.string1)
    }

    private fun writeSeasonalCalendarDailyOfferOutList1ItemBranch1ElseBranch1Then(w: PacketWriter, m: SeasonalCalendarDailyOfferOut.List1Item.Branch1.Else.Branch1.Then) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writeSeasonalCalendarDailyOfferOutList1ItemBranch1Else(w: PacketWriter, m: SeasonalCalendarDailyOfferOut.List1Item.Branch1.Else) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.boolean(m.boolean1)
        when (val b = m.branch1) {
            is SeasonalCalendarDailyOfferOut.List1Item.Branch1.Else.Branch1.Then -> writeSeasonalCalendarDailyOfferOutList1ItemBranch1ElseBranch1Then(w, b)
            is SeasonalCalendarDailyOfferOut.List1Item.Branch1.Else.Branch1.Else -> {}
        }
    }

    private fun writeSeasonalCalendarDailyOfferOutList1Item(w: PacketWriter, m: SeasonalCalendarDailyOfferOut.List1Item) {
        w.string(m.string1)
        when (val b = m.branch1) {
            is SeasonalCalendarDailyOfferOut.List1Item.Branch1.Then -> writeSeasonalCalendarDailyOfferOutList1ItemBranch1Then(w, b)
            is SeasonalCalendarDailyOfferOut.List1Item.Branch1.Else -> writeSeasonalCalendarDailyOfferOutList1ItemBranch1Else(w, b)
        }
    }

    private fun writeSeasonalCalendarDailyOfferOut(w: PacketWriter, m: SeasonalCalendarDailyOfferOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.boolean(m.boolean1)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.boolean(m.boolean2)
        w.countInt(m.list1.size)
        m.list1.forEach { writeSeasonalCalendarDailyOfferOutList1Item(w, it) }
        w.int(m.int7)
        w.boolean(m.boolean3)
        w.boolean(m.boolean4)
        w.string(m.string2)
    }
}
