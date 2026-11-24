package zel.protocol.generated

data class GroupBadgesOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
    )
}

object GroupBadgesOutCodec : OutgoingCodec<GroupBadgesOut> {
    override val name = "GroupBadgesOut"

    override fun read(r: PacketReader): GroupBadgesOut = readGroupBadgesOut(r)

    override fun write(w: PacketWriter, m: GroupBadgesOut) = writeGroupBadgesOut(w, m)

    private fun readGroupBadgesOutList1Item(r: PacketReader): GroupBadgesOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return GroupBadgesOut.List1Item(vInt1, vString1)
    }

    private fun readGroupBadgesOut(r: PacketReader): GroupBadgesOut {
        val vList1 = List(r.countInt(6)) { readGroupBadgesOutList1Item(r) }
        return GroupBadgesOut(vList1)
    }

    private fun writeGroupBadgesOutList1Item(w: PacketWriter, m: GroupBadgesOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeGroupBadgesOut(w: PacketWriter, m: GroupBadgesOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeGroupBadgesOutList1Item(w, it) }
    }
}
