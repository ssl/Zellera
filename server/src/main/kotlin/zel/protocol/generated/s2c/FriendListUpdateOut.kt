package zel.protocol.generated

data class FriendListUpdateOut(
    val list1: List<List1Item>,
    val rest: ByteArray,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
    )
}

object FriendListUpdateOutCodec : OutgoingCodec<FriendListUpdateOut> {
    override val name = "FriendListUpdateOut"

    override fun read(r: PacketReader): FriendListUpdateOut = readFriendListUpdateOut(r)

    override fun write(w: PacketWriter, m: FriendListUpdateOut) = writeFriendListUpdateOut(w, m)

    private fun readFriendListUpdateOutList1Item(r: PacketReader): FriendListUpdateOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return FriendListUpdateOut.List1Item(vInt1, vString1)
    }

    private fun readFriendListUpdateOut(r: PacketReader): FriendListUpdateOut {
        val vList1 = List(r.countInt(6)) { readFriendListUpdateOutList1Item(r) }
        val vRest = r.rest()
        return FriendListUpdateOut(vList1, vRest)
    }

    private fun writeFriendListUpdateOutList1Item(w: PacketWriter, m: FriendListUpdateOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeFriendListUpdateOut(w: PacketWriter, m: FriendListUpdateOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeFriendListUpdateOutList1Item(w, it) }
        w.bytes(m.rest)
    }
}
