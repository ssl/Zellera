package zel.protocol.generated

data class Game2TotalLeaderboardOut(
    val list1: List<List1Item>,
    val int1: Int,
    val int2: Int,
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

object Game2TotalLeaderboardOutCodec : OutgoingCodec<Game2TotalLeaderboardOut> {
    override val name = "Game2TotalLeaderboardOut"

    override fun read(r: PacketReader): Game2TotalLeaderboardOut = readGame2TotalLeaderboardOut(r)

    override fun write(w: PacketWriter, m: Game2TotalLeaderboardOut) = writeGame2TotalLeaderboardOut(w, m)

    private fun readGame2TotalLeaderboardOutList1Item(r: PacketReader): Game2TotalLeaderboardOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return Game2TotalLeaderboardOut.List1Item(vInt1, vInt2, vInt3, vString1, vString2, vString3)
    }

    private fun readGame2TotalLeaderboardOut(r: PacketReader): Game2TotalLeaderboardOut {
        val vList1 = List(r.countInt(18)) { readGame2TotalLeaderboardOutList1Item(r) }
        val vInt1 = r.int()
        val vInt2 = r.int()
        return Game2TotalLeaderboardOut(vList1, vInt1, vInt2)
    }

    private fun writeGame2TotalLeaderboardOutList1Item(w: PacketWriter, m: Game2TotalLeaderboardOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }

    private fun writeGame2TotalLeaderboardOut(w: PacketWriter, m: Game2TotalLeaderboardOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeGame2TotalLeaderboardOutList1Item(w, it) }
        w.int(m.int1)
        w.int(m.int2)
    }
}
