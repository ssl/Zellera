package zel.protocol.generated

data class Game2TotalGroupLeaderboardOut(
    val list1: List<List1Item>,
    val int1: Int,
    val int2: Int,
    val int3: Int,
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

object Game2TotalGroupLeaderboardOutCodec : OutgoingCodec<Game2TotalGroupLeaderboardOut> {
    override val name = "Game2TotalGroupLeaderboardOut"

    override fun read(r: PacketReader): Game2TotalGroupLeaderboardOut = readGame2TotalGroupLeaderboardOut(r)

    override fun write(w: PacketWriter, m: Game2TotalGroupLeaderboardOut) = writeGame2TotalGroupLeaderboardOut(w, m)

    private fun readGame2TotalGroupLeaderboardOutList1Item(r: PacketReader): Game2TotalGroupLeaderboardOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return Game2TotalGroupLeaderboardOut.List1Item(vInt1, vInt2, vInt3, vString1, vString2, vString3)
    }

    private fun readGame2TotalGroupLeaderboardOut(r: PacketReader): Game2TotalGroupLeaderboardOut {
        val vList1 = List(r.countInt(18)) { readGame2TotalGroupLeaderboardOutList1Item(r) }
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return Game2TotalGroupLeaderboardOut(vList1, vInt1, vInt2, vInt3)
    }

    private fun writeGame2TotalGroupLeaderboardOutList1Item(w: PacketWriter, m: Game2TotalGroupLeaderboardOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }

    private fun writeGame2TotalGroupLeaderboardOut(w: PacketWriter, m: Game2TotalGroupLeaderboardOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeGame2TotalGroupLeaderboardOutList1Item(w, it) }
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
