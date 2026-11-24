package zel.protocol.generated

data class AcceptFriendResultOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
    )
}

object AcceptFriendResultOutCodec : OutgoingCodec<AcceptFriendResultOut> {
    override val name = "AcceptFriendResultOut"

    override fun read(r: PacketReader): AcceptFriendResultOut = readAcceptFriendResultOut(r)

    override fun write(w: PacketWriter, m: AcceptFriendResultOut) = writeAcceptFriendResultOut(w, m)

    private fun readAcceptFriendResultOutList1Item(r: PacketReader): AcceptFriendResultOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return AcceptFriendResultOut.List1Item(vInt1, vInt2)
    }

    private fun readAcceptFriendResultOut(r: PacketReader): AcceptFriendResultOut {
        val vList1 = List(r.countInt(8)) { readAcceptFriendResultOutList1Item(r) }
        return AcceptFriendResultOut(vList1)
    }

    private fun writeAcceptFriendResultOutList1Item(w: PacketWriter, m: AcceptFriendResultOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writeAcceptFriendResultOut(w: PacketWriter, m: AcceptFriendResultOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeAcceptFriendResultOutList1Item(w, it) }
    }
}
