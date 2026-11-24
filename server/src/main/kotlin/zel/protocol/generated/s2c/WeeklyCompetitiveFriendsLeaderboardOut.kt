package zel.protocol.generated

data class WeeklyCompetitiveFriendsLeaderboardOut(
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

object WeeklyCompetitiveFriendsLeaderboardOutCodec : OutgoingCodec<WeeklyCompetitiveFriendsLeaderboardOut> {
    override val name = "WeeklyCompetitiveFriendsLeaderboardOut"

    override fun read(r: PacketReader): WeeklyCompetitiveFriendsLeaderboardOut = readWeeklyCompetitiveFriendsLeaderboardOut(r)

    override fun write(w: PacketWriter, m: WeeklyCompetitiveFriendsLeaderboardOut) = writeWeeklyCompetitiveFriendsLeaderboardOut(w, m)

    private fun readWeeklyCompetitiveFriendsLeaderboardOutList1Item(r: PacketReader): WeeklyCompetitiveFriendsLeaderboardOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return WeeklyCompetitiveFriendsLeaderboardOut.List1Item(vInt1, vInt2, vInt3, vString1, vString2, vString3)
    }

    private fun readWeeklyCompetitiveFriendsLeaderboardOut(r: PacketReader): WeeklyCompetitiveFriendsLeaderboardOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vList1 = List(r.countInt(18)) { readWeeklyCompetitiveFriendsLeaderboardOutList1Item(r) }
        val vInt6 = r.int()
        val vInt7 = r.int()
        return WeeklyCompetitiveFriendsLeaderboardOut(vInt1, vInt2, vInt3, vInt4, vInt5, vList1, vInt6, vInt7)
    }

    private fun writeWeeklyCompetitiveFriendsLeaderboardOutList1Item(w: PacketWriter, m: WeeklyCompetitiveFriendsLeaderboardOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }

    private fun writeWeeklyCompetitiveFriendsLeaderboardOut(w: PacketWriter, m: WeeklyCompetitiveFriendsLeaderboardOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.countInt(m.list1.size)
        m.list1.forEach { writeWeeklyCompetitiveFriendsLeaderboardOutList1Item(w, it) }
        w.int(m.int6)
        w.int(m.int7)
    }
}
