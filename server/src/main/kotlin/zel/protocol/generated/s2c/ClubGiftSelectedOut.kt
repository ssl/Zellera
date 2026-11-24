package zel.protocol.generated

data class ClubGiftSelectedOut(
    val string1: String,
    val list1: List<List1Item>,
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

object ClubGiftSelectedOutCodec : OutgoingCodec<ClubGiftSelectedOut> {
    override val name = "ClubGiftSelectedOut"

    override fun read(r: PacketReader): ClubGiftSelectedOut = readClubGiftSelectedOut(r)

    override fun write(w: PacketWriter, m: ClubGiftSelectedOut) = writeClubGiftSelectedOut(w, m)

    private fun readClubGiftSelectedOutList1ItemBranch1Then(r: PacketReader): ClubGiftSelectedOut.List1Item.Branch1.Then {
        val vString1 = r.string()
        return ClubGiftSelectedOut.List1Item.Branch1.Then(vString1)
    }

    private fun readClubGiftSelectedOutList1ItemBranch1ElseBranch1Then(r: PacketReader): ClubGiftSelectedOut.List1Item.Branch1.Else.Branch1.Then {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return ClubGiftSelectedOut.List1Item.Branch1.Else.Branch1.Then(vInt1, vInt2)
    }

    private fun readClubGiftSelectedOutList1ItemBranch1Else(r: PacketReader): ClubGiftSelectedOut.List1Item.Branch1.Else {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        val vBranch1 = if (vBoolean1 == true) readClubGiftSelectedOutList1ItemBranch1ElseBranch1Then(r) else ClubGiftSelectedOut.List1Item.Branch1.Else.Branch1.Else
        return ClubGiftSelectedOut.List1Item.Branch1.Else(vInt1, vString1, vInt2, vBoolean1, vBranch1)
    }

    private fun readClubGiftSelectedOutList1Item(r: PacketReader): ClubGiftSelectedOut.List1Item {
        val vString1 = r.string()
        val vBranch1 = if (vString1 == "b") readClubGiftSelectedOutList1ItemBranch1Then(r) else readClubGiftSelectedOutList1ItemBranch1Else(r)
        return ClubGiftSelectedOut.List1Item(vString1, vBranch1)
    }

    private fun readClubGiftSelectedOut(r: PacketReader): ClubGiftSelectedOut {
        val vString1 = r.string()
        val vList1 = List(r.countInt(4)) { readClubGiftSelectedOutList1Item(r) }
        return ClubGiftSelectedOut(vString1, vList1)
    }

    private fun writeClubGiftSelectedOutList1ItemBranch1Then(w: PacketWriter, m: ClubGiftSelectedOut.List1Item.Branch1.Then) {
        w.string(m.string1)
    }

    private fun writeClubGiftSelectedOutList1ItemBranch1ElseBranch1Then(w: PacketWriter, m: ClubGiftSelectedOut.List1Item.Branch1.Else.Branch1.Then) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writeClubGiftSelectedOutList1ItemBranch1Else(w: PacketWriter, m: ClubGiftSelectedOut.List1Item.Branch1.Else) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.boolean(m.boolean1)
        when (val b = m.branch1) {
            is ClubGiftSelectedOut.List1Item.Branch1.Else.Branch1.Then -> writeClubGiftSelectedOutList1ItemBranch1ElseBranch1Then(w, b)
            is ClubGiftSelectedOut.List1Item.Branch1.Else.Branch1.Else -> {}
        }
    }

    private fun writeClubGiftSelectedOutList1Item(w: PacketWriter, m: ClubGiftSelectedOut.List1Item) {
        w.string(m.string1)
        when (val b = m.branch1) {
            is ClubGiftSelectedOut.List1Item.Branch1.Then -> writeClubGiftSelectedOutList1ItemBranch1Then(w, b)
            is ClubGiftSelectedOut.List1Item.Branch1.Else -> writeClubGiftSelectedOutList1ItemBranch1Else(w, b)
        }
    }

    private fun writeClubGiftSelectedOut(w: PacketWriter, m: ClubGiftSelectedOut) {
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeClubGiftSelectedOutList1Item(w, it) }
    }
}
