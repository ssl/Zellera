package zel.protocol.generated

data class BannedPlayersFromRoomOut(
    val int1: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
    )
}

object BannedPlayersFromRoomOutCodec : OutgoingCodec<BannedPlayersFromRoomOut> {
    override val name = "BannedPlayersFromRoomOut"

    override fun read(r: PacketReader): BannedPlayersFromRoomOut = readBannedPlayersFromRoomOut(r)

    override fun write(w: PacketWriter, m: BannedPlayersFromRoomOut) = writeBannedPlayersFromRoomOut(w, m)

    private fun readBannedPlayersFromRoomOutList1Item(r: PacketReader): BannedPlayersFromRoomOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return BannedPlayersFromRoomOut.List1Item(vInt1, vString1)
    }

    private fun readBannedPlayersFromRoomOut(r: PacketReader): BannedPlayersFromRoomOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(6)) { readBannedPlayersFromRoomOutList1Item(r) }
        return BannedPlayersFromRoomOut(vInt1, vList1)
    }

    private fun writeBannedPlayersFromRoomOutList1Item(w: PacketWriter, m: BannedPlayersFromRoomOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeBannedPlayersFromRoomOut(w: PacketWriter, m: BannedPlayersFromRoomOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeBannedPlayersFromRoomOutList1Item(w, it) }
    }
}
