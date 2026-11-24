package zel.protocol.generated

data class WeeklyCompetitiveLeaderboardOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val list1: List<List1Item>,
    val int6: Int,
    val int7: Int,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
        val string1: String,
        val string2: String,
        val string3: String,
    )
}

object WeeklyCompetitiveLeaderboardOutCodec : OutgoingCodec<WeeklyCompetitiveLeaderboardOut> {
    override val name = "WeeklyCompetitiveLeaderboardOut"

    override fun read(r: PacketReader): WeeklyCompetitiveLeaderboardOut = readWeeklyCompetitiveLeaderboardOut(r)

    override fun write(w: PacketWriter, m: WeeklyCompetitiveLeaderboardOut) = writeWeeklyCompetitiveLeaderboardOut(w, m)

    private fun readWeeklyCompetitiveLeaderboardOutList1Item(r: PacketReader): WeeklyCompetitiveLeaderboardOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return WeeklyCompetitiveLeaderboardOut.List1Item(vInt1, vInt2, vInt3, vString1, vString2, vString3)
    }

    private fun readWeeklyCompetitiveLeaderboardOut(r: PacketReader): WeeklyCompetitiveLeaderboardOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vList1 = List(r.countInt(18)) { readWeeklyCompetitiveLeaderboardOutList1Item(r) }
        val vInt6 = r.int()
        val vInt7 = r.int()
        return WeeklyCompetitiveLeaderboardOut(vInt1, vInt2, vInt3, vInt4, vInt5, vList1, vInt6, vInt7)
    }

    private fun writeWeeklyCompetitiveLeaderboardOutList1Item(w: PacketWriter, m: WeeklyCompetitiveLeaderboardOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }

    private fun writeWeeklyCompetitiveLeaderboardOut(w: PacketWriter, m: WeeklyCompetitiveLeaderboardOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.countInt(m.list1.size)
        m.list1.forEach { writeWeeklyCompetitiveLeaderboardOutList1Item(w, it) }
        w.int(m.int6)
        w.int(m.int7)
    }
}
