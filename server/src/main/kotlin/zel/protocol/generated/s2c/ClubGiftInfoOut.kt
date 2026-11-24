package zel.protocol.generated

data class ClubGiftInfoOut(
    val int1: Int,
    val int2: Int,
    val list1: List<List1Item>,
    val list2: List<List2Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val boolean1: Boolean,
        val int2: Int,
        val int3: Int,
        val int4: Int,
        val int5: Int,
        val boolean2: Boolean,
        val list1: List<List1Item>,
        val int6: Int,
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

    data class List2Item(
        val int1: Int,
        val boolean1: Boolean,
        val int2: Int,
        val boolean2: Boolean,
    )
}

object ClubGiftInfoOutCodec : OutgoingCodec<ClubGiftInfoOut> {
    override val name = "ClubGiftInfoOut"

    override fun read(r: PacketReader): ClubGiftInfoOut = readClubGiftInfoOut(r)

    override fun write(w: PacketWriter, m: ClubGiftInfoOut) = writeClubGiftInfoOut(w, m)

    private fun readClubGiftInfoOutList1ItemList1ItemBranch1Then(r: PacketReader): ClubGiftInfoOut.List1Item.List1Item.Branch1.Then {
        val vString1 = r.string()
        return ClubGiftInfoOut.List1Item.List1Item.Branch1.Then(vString1)
    }

    private fun readClubGiftInfoOutList1ItemList1ItemBranch1ElseBranch1Then(r: PacketReader): ClubGiftInfoOut.List1Item.List1Item.Branch1.Else.Branch1.Then {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return ClubGiftInfoOut.List1Item.List1Item.Branch1.Else.Branch1.Then(vInt1, vInt2)
    }

    private fun readClubGiftInfoOutList1ItemList1ItemBranch1Else(r: PacketReader): ClubGiftInfoOut.List1Item.List1Item.Branch1.Else {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        val vBranch1 = if (vBoolean1 == true) readClubGiftInfoOutList1ItemList1ItemBranch1ElseBranch1Then(r) else ClubGiftInfoOut.List1Item.List1Item.Branch1.Else.Branch1.Else
        return ClubGiftInfoOut.List1Item.List1Item.Branch1.Else(vInt1, vString1, vInt2, vBoolean1, vBranch1)
    }

    private fun readClubGiftInfoOutList1ItemList1Item(r: PacketReader): ClubGiftInfoOut.List1Item.List1Item {
        val vString1 = r.string()
        val vBranch1 = if (vString1 == "b") readClubGiftInfoOutList1ItemList1ItemBranch1Then(r) else readClubGiftInfoOutList1ItemList1ItemBranch1Else(r)
        return ClubGiftInfoOut.List1Item.List1Item(vString1, vBranch1)
    }

    private fun readClubGiftInfoOutList1Item(r: PacketReader): ClubGiftInfoOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vBoolean2 = r.boolean()
        val vList1 = List(r.countInt(4)) { readClubGiftInfoOutList1ItemList1Item(r) }
        val vInt6 = r.int()
        val vBoolean3 = r.boolean()
        val vBoolean4 = r.boolean()
        val vString2 = r.string()
        return ClubGiftInfoOut.List1Item(vInt1, vString1, vBoolean1, vInt2, vInt3, vInt4, vInt5, vBoolean2, vList1, vInt6, vBoolean3, vBoolean4, vString2)
    }

    private fun readClubGiftInfoOutList2Item(r: PacketReader): ClubGiftInfoOut.List2Item {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vBoolean2 = r.boolean()
        return ClubGiftInfoOut.List2Item(vInt1, vBoolean1, vInt2, vBoolean2)
    }

    private fun readClubGiftInfoOut(r: PacketReader): ClubGiftInfoOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vList1 = List(r.countInt(36)) { readClubGiftInfoOutList1Item(r) }
        val vList2 = List(r.countInt(10)) { readClubGiftInfoOutList2Item(r) }
        return ClubGiftInfoOut(vInt1, vInt2, vList1, vList2)
    }

    private fun writeClubGiftInfoOutList1ItemList1ItemBranch1Then(w: PacketWriter, m: ClubGiftInfoOut.List1Item.List1Item.Branch1.Then) {
        w.string(m.string1)
    }

    private fun writeClubGiftInfoOutList1ItemList1ItemBranch1ElseBranch1Then(w: PacketWriter, m: ClubGiftInfoOut.List1Item.List1Item.Branch1.Else.Branch1.Then) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writeClubGiftInfoOutList1ItemList1ItemBranch1Else(w: PacketWriter, m: ClubGiftInfoOut.List1Item.List1Item.Branch1.Else) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.boolean(m.boolean1)
        when (val b = m.branch1) {
            is ClubGiftInfoOut.List1Item.List1Item.Branch1.Else.Branch1.Then -> writeClubGiftInfoOutList1ItemList1ItemBranch1ElseBranch1Then(w, b)
            is ClubGiftInfoOut.List1Item.List1Item.Branch1.Else.Branch1.Else -> {}
        }
    }

    private fun writeClubGiftInfoOutList1ItemList1Item(w: PacketWriter, m: ClubGiftInfoOut.List1Item.List1Item) {
        w.string(m.string1)
        when (val b = m.branch1) {
            is ClubGiftInfoOut.List1Item.List1Item.Branch1.Then -> writeClubGiftInfoOutList1ItemList1ItemBranch1Then(w, b)
            is ClubGiftInfoOut.List1Item.List1Item.Branch1.Else -> writeClubGiftInfoOutList1ItemList1ItemBranch1Else(w, b)
        }
    }

    private fun writeClubGiftInfoOutList1Item(w: PacketWriter, m: ClubGiftInfoOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.boolean(m.boolean2)
        w.countInt(m.list1.size)
        m.list1.forEach { writeClubGiftInfoOutList1ItemList1Item(w, it) }
        w.int(m.int6)
        w.boolean(m.boolean3)
        w.boolean(m.boolean4)
        w.string(m.string2)
    }

    private fun writeClubGiftInfoOutList2Item(w: PacketWriter, m: ClubGiftInfoOut.List2Item) {
        w.int(m.int1)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.boolean(m.boolean2)
    }

    private fun writeClubGiftInfoOut(w: PacketWriter, m: ClubGiftInfoOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.countInt(m.list1.size)
        m.list1.forEach { writeClubGiftInfoOutList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { writeClubGiftInfoOutList2Item(w, it) }
    }
}
