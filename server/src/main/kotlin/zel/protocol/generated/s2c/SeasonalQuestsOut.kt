package zel.protocol.generated

data class SeasonalQuestsOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val int1: Int,
        val int2: Int,
        val int3: Int,
        val int4: Int,
        val boolean1: Boolean,
        val string2: String,
        val string3: String,
        val int5: Int,
        val string4: String,
        val int6: Int,
        val int7: Int,
        val int8: Int,
        val string5: String,
        val string6: String,
        val boolean2: Boolean,
        val boolean3: Boolean,
        val branch1: Branch1,
    ) {
        sealed interface Branch1 {
            data class Then(
                val int1: Int,
            ) : Branch1
            data object Else : Branch1
        }
    }
}

object SeasonalQuestsOutCodec : OutgoingCodec<SeasonalQuestsOut> {
    override val name = "SeasonalQuestsOut"

    override fun read(r: PacketReader): SeasonalQuestsOut = readSeasonalQuestsOut(r)

    override fun write(w: PacketWriter, m: SeasonalQuestsOut) = writeSeasonalQuestsOut(w, m)

    private fun readSeasonalQuestsOutList1ItemBranch1Then(r: PacketReader): SeasonalQuestsOut.List1Item.Branch1.Then {
        val vInt1 = r.int()
        return SeasonalQuestsOut.List1Item.Branch1.Then(vInt1)
    }

    private fun readSeasonalQuestsOutList1Item(r: PacketReader): SeasonalQuestsOut.List1Item {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vBoolean1 = r.boolean()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt5 = r.int()
        val vString4 = r.string()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vString5 = r.string()
        val vString6 = r.string()
        val vBoolean2 = r.boolean()
        val vBoolean3 = r.boolean()
        val vBranch1 = if (vBoolean3 == true) readSeasonalQuestsOutList1ItemBranch1Then(r) else SeasonalQuestsOut.List1Item.Branch1.Else
        return SeasonalQuestsOut.List1Item(vString1, vInt1, vInt2, vInt3, vInt4, vBoolean1, vString2, vString3, vInt5, vString4, vInt6, vInt7, vInt8, vString5, vString6, vBoolean2, vBoolean3, vBranch1)
    }

    private fun readSeasonalQuestsOut(r: PacketReader): SeasonalQuestsOut {
        val vList1 = List(r.countInt(47)) { readSeasonalQuestsOutList1Item(r) }
        return SeasonalQuestsOut(vList1)
    }

    private fun writeSeasonalQuestsOutList1ItemBranch1Then(w: PacketWriter, m: SeasonalQuestsOut.List1Item.Branch1.Then) {
        w.int(m.int1)
    }

    private fun writeSeasonalQuestsOutList1Item(w: PacketWriter, m: SeasonalQuestsOut.List1Item) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.boolean(m.boolean1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int5)
        w.string(m.string4)
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.string(m.string5)
        w.string(m.string6)
        w.boolean(m.boolean2)
        w.boolean(m.boolean3)
        when (val b = m.branch1) {
            is SeasonalQuestsOut.List1Item.Branch1.Then -> writeSeasonalQuestsOutList1ItemBranch1Then(w, b)
            is SeasonalQuestsOut.List1Item.Branch1.Else -> {}
        }
    }

    private fun writeSeasonalQuestsOut(w: PacketWriter, m: SeasonalQuestsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeSeasonalQuestsOutList1Item(w, it) }
    }
}
