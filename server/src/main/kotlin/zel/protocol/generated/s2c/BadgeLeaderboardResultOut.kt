package zel.protocol.generated

data class BadgeLeaderboardResultOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val list1: List<List1Item>,
    val boolean1: Boolean,
    val branch1: Branch1,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val string2: String,
        val int2: Int,
        val int3: Int,
    )

    sealed interface Branch1 {
        data class Then(
            val int1: Int,
            val string1: String,
            val string2: String,
            val int2: Int,
            val int3: Int,
        ) : Branch1
        data object Else : Branch1
    }
}

object BadgeLeaderboardResultOutCodec : OutgoingCodec<BadgeLeaderboardResultOut> {
    override val name = "BadgeLeaderboardResultOut"

    override fun read(r: PacketReader): BadgeLeaderboardResultOut = readBadgeLeaderboardResultOut(r)

    override fun write(w: PacketWriter, m: BadgeLeaderboardResultOut) = writeBadgeLeaderboardResultOut(w, m)

    private fun readBadgeLeaderboardResultOutList1Item(r: PacketReader): BadgeLeaderboardResultOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return BadgeLeaderboardResultOut.List1Item(vInt1, vString1, vString2, vInt2, vInt3)
    }

    private fun readBadgeLeaderboardResultOutBranch1Then(r: PacketReader): BadgeLeaderboardResultOut.Branch1.Then {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return BadgeLeaderboardResultOut.Branch1.Then(vInt1, vString1, vString2, vInt2, vInt3)
    }

    private fun readBadgeLeaderboardResultOut(r: PacketReader): BadgeLeaderboardResultOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vList1 = List(r.countInt(16)) { readBadgeLeaderboardResultOutList1Item(r) }
        val vBoolean1 = r.boolean()
        val vBranch1 = if (vBoolean1 == true) readBadgeLeaderboardResultOutBranch1Then(r) else BadgeLeaderboardResultOut.Branch1.Else
        return BadgeLeaderboardResultOut(vInt1, vInt2, vInt3, vInt4, vInt5, vList1, vBoolean1, vBranch1)
    }

    private fun writeBadgeLeaderboardResultOutList1Item(w: PacketWriter, m: BadgeLeaderboardResultOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int2)
        w.int(m.int3)
    }

    private fun writeBadgeLeaderboardResultOutBranch1Then(w: PacketWriter, m: BadgeLeaderboardResultOut.Branch1.Then) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int2)
        w.int(m.int3)
    }

    private fun writeBadgeLeaderboardResultOut(w: PacketWriter, m: BadgeLeaderboardResultOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.countInt(m.list1.size)
        m.list1.forEach { writeBadgeLeaderboardResultOutList1Item(w, it) }
        w.boolean(m.boolean1)
        when (val b = m.branch1) {
            is BadgeLeaderboardResultOut.Branch1.Then -> writeBadgeLeaderboardResultOutBranch1Then(w, b)
            is BadgeLeaderboardResultOut.Branch1.Else -> {}
        }
    }
}
