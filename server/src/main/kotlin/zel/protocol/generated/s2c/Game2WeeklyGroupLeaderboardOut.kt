package zel.protocol.generated

data class Game2WeeklyGroupLeaderboardOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val list1: List<List1Item>,
    val int6: Int,
    val int7: Int,
    val int8: Int,
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

object Game2WeeklyGroupLeaderboardOutCodec : OutgoingCodec<Game2WeeklyGroupLeaderboardOut> {
    override val name = "Game2WeeklyGroupLeaderboardOut"

    override fun read(r: PacketReader): Game2WeeklyGroupLeaderboardOut = readGame2WeeklyGroupLeaderboardOut(r)

    override fun write(w: PacketWriter, m: Game2WeeklyGroupLeaderboardOut) = writeGame2WeeklyGroupLeaderboardOut(w, m)

    private fun readGame2WeeklyGroupLeaderboardOutList1Item(r: PacketReader): Game2WeeklyGroupLeaderboardOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return Game2WeeklyGroupLeaderboardOut.List1Item(vInt1, vInt2, vInt3, vString1, vString2, vString3)
    }

    private fun readGame2WeeklyGroupLeaderboardOut(r: PacketReader): Game2WeeklyGroupLeaderboardOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vList1 = List(r.countInt(18)) { readGame2WeeklyGroupLeaderboardOutList1Item(r) }
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        return Game2WeeklyGroupLeaderboardOut(vInt1, vInt2, vInt3, vInt4, vInt5, vList1, vInt6, vInt7, vInt8)
    }

    private fun writeGame2WeeklyGroupLeaderboardOutList1Item(w: PacketWriter, m: Game2WeeklyGroupLeaderboardOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }

    private fun writeGame2WeeklyGroupLeaderboardOut(w: PacketWriter, m: Game2WeeklyGroupLeaderboardOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.countInt(m.list1.size)
        m.list1.forEach { writeGame2WeeklyGroupLeaderboardOutList1Item(w, it) }
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
    }
}
