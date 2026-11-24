package zel.protocol.generated

data class Game2WeeklyFriendsLeaderboardOut(
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

object Game2WeeklyFriendsLeaderboardOutCodec : OutgoingCodec<Game2WeeklyFriendsLeaderboardOut> {
    override val name = "Game2WeeklyFriendsLeaderboardOut"

    override fun read(r: PacketReader): Game2WeeklyFriendsLeaderboardOut = readGame2WeeklyFriendsLeaderboardOut(r)

    override fun write(w: PacketWriter, m: Game2WeeklyFriendsLeaderboardOut) = writeGame2WeeklyFriendsLeaderboardOut(w, m)

    private fun readGame2WeeklyFriendsLeaderboardOutList1Item(r: PacketReader): Game2WeeklyFriendsLeaderboardOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return Game2WeeklyFriendsLeaderboardOut.List1Item(vInt1, vInt2, vInt3, vString1, vString2, vString3)
    }

    private fun readGame2WeeklyFriendsLeaderboardOut(r: PacketReader): Game2WeeklyFriendsLeaderboardOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vList1 = List(r.countInt(18)) { readGame2WeeklyFriendsLeaderboardOutList1Item(r) }
        val vInt6 = r.int()
        val vInt7 = r.int()
        return Game2WeeklyFriendsLeaderboardOut(vInt1, vInt2, vInt3, vInt4, vInt5, vList1, vInt6, vInt7)
    }

    private fun writeGame2WeeklyFriendsLeaderboardOutList1Item(w: PacketWriter, m: Game2WeeklyFriendsLeaderboardOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }

    private fun writeGame2WeeklyFriendsLeaderboardOut(w: PacketWriter, m: Game2WeeklyFriendsLeaderboardOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.countInt(m.list1.size)
        m.list1.forEach { writeGame2WeeklyFriendsLeaderboardOutList1Item(w, it) }
        w.int(m.int6)
        w.int(m.int7)
    }
}
