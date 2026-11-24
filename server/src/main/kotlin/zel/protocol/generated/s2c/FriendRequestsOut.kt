package zel.protocol.generated

data class FriendRequestsOut(
    val int1: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val string2: String,
    )
}

object FriendRequestsOutCodec : OutgoingCodec<FriendRequestsOut> {
    override val name = "FriendRequestsOut"

    override fun read(r: PacketReader): FriendRequestsOut = readFriendRequestsOut(r)

    override fun write(w: PacketWriter, m: FriendRequestsOut) = writeFriendRequestsOut(w, m)

    private fun readFriendRequestsOutList1Item(r: PacketReader): FriendRequestsOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        return FriendRequestsOut.List1Item(vInt1, vString1, vString2)
    }

    private fun readFriendRequestsOut(r: PacketReader): FriendRequestsOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(8)) { readFriendRequestsOutList1Item(r) }
        return FriendRequestsOut(vInt1, vList1)
    }

    private fun writeFriendRequestsOutList1Item(w: PacketWriter, m: FriendRequestsOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
    }

    private fun writeFriendRequestsOut(w: PacketWriter, m: FriendRequestsOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeFriendRequestsOutList1Item(w, it) }
    }
}
