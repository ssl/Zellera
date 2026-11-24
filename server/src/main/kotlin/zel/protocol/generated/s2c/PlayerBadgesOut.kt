package zel.protocol.generated

data class PlayerBadgesOut(
    val int1: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val int2: Int,
        val int3: Int,
    )
}

object PlayerBadgesOutCodec : OutgoingCodec<PlayerBadgesOut> {
    override val name = "PlayerBadgesOut"

    override fun read(r: PacketReader): PlayerBadgesOut = readPlayerBadgesOut(r)

    override fun write(w: PacketWriter, m: PlayerBadgesOut) = writePlayerBadgesOut(w, m)

    private fun readPlayerBadgesOutList1Item(r: PacketReader): PlayerBadgesOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return PlayerBadgesOut.List1Item(vInt1, vString1, vInt2, vInt3)
    }

    private fun readPlayerBadgesOut(r: PacketReader): PlayerBadgesOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(14)) { readPlayerBadgesOutList1Item(r) }
        return PlayerBadgesOut(vInt1, vList1)
    }

    private fun writePlayerBadgesOutList1Item(w: PacketWriter, m: PlayerBadgesOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
    }

    private fun writePlayerBadgesOut(w: PacketWriter, m: PlayerBadgesOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writePlayerBadgesOutList1Item(w, it) }
    }
}
