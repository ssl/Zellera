package zel.protocol.generated

data class Game2FriendsLeaderboardOut(
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

object Game2FriendsLeaderboardOutCodec : OutgoingCodec<Game2FriendsLeaderboardOut> {
    override val name = "Game2FriendsLeaderboardOut"

    override fun read(r: PacketReader): Game2FriendsLeaderboardOut = readGame2FriendsLeaderboardOut(r)

    override fun write(w: PacketWriter, m: Game2FriendsLeaderboardOut) = writeGame2FriendsLeaderboardOut(w, m)

    private fun readGame2FriendsLeaderboardOutList1Item(r: PacketReader): Game2FriendsLeaderboardOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return Game2FriendsLeaderboardOut.List1Item(vInt1, vInt2, vInt3, vString1, vString2, vString3)
    }

    private fun readGame2FriendsLeaderboardOut(r: PacketReader): Game2FriendsLeaderboardOut {
        val vList1 = List(r.countInt(18)) { readGame2FriendsLeaderboardOutList1Item(r) }
        val vInt1 = r.int()
        val vInt2 = r.int()
        return Game2FriendsLeaderboardOut(vList1, vInt1, vInt2)
    }

    private fun writeGame2FriendsLeaderboardOutList1Item(w: PacketWriter, m: Game2FriendsLeaderboardOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }

    private fun writeGame2FriendsLeaderboardOut(w: PacketWriter, m: Game2FriendsLeaderboardOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeGame2FriendsLeaderboardOutList1Item(w, it) }
        w.int(m.int1)
        w.int(m.int2)
    }
}
