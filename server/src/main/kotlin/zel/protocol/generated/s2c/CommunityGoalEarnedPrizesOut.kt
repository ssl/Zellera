package zel.protocol.generated

data class CommunityGoalEarnedPrizesOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val int2: Int,
        val string2: String,
        val boolean1: Boolean,
        val string3: String,
    )
}

object CommunityGoalEarnedPrizesOutCodec : OutgoingCodec<CommunityGoalEarnedPrizesOut> {
    override val name = "CommunityGoalEarnedPrizesOut"

    override fun read(r: PacketReader): CommunityGoalEarnedPrizesOut = readCommunityGoalEarnedPrizesOut(r)

    override fun write(w: PacketWriter, m: CommunityGoalEarnedPrizesOut) = writeCommunityGoalEarnedPrizesOut(w, m)

    private fun readCommunityGoalEarnedPrizesOutList1Item(r: PacketReader): CommunityGoalEarnedPrizesOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vString2 = r.string()
        val vBoolean1 = r.boolean()
        val vString3 = r.string()
        return CommunityGoalEarnedPrizesOut.List1Item(vInt1, vString1, vInt2, vString2, vBoolean1, vString3)
    }

    private fun readCommunityGoalEarnedPrizesOut(r: PacketReader): CommunityGoalEarnedPrizesOut {
        val vList1 = List(r.countInt(15)) { readCommunityGoalEarnedPrizesOutList1Item(r) }
        return CommunityGoalEarnedPrizesOut(vList1)
    }

    private fun writeCommunityGoalEarnedPrizesOutList1Item(w: PacketWriter, m: CommunityGoalEarnedPrizesOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.string(m.string2)
        w.boolean(m.boolean1)
        w.string(m.string3)
    }

    private fun writeCommunityGoalEarnedPrizesOut(w: PacketWriter, m: CommunityGoalEarnedPrizesOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeCommunityGoalEarnedPrizesOutList1Item(w, it) }
    }
}
