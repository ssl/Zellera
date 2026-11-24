package zel.protocol.generated

data class PlayerSongDisksInventoryOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
    )
}

object PlayerSongDisksInventoryOutCodec : OutgoingCodec<PlayerSongDisksInventoryOut> {
    override val name = "PlayerSongDisksInventoryOut"

    override fun read(r: PacketReader): PlayerSongDisksInventoryOut = readPlayerSongDisksInventoryOut(r)

    override fun write(w: PacketWriter, m: PlayerSongDisksInventoryOut) = writePlayerSongDisksInventoryOut(w, m)

    private fun readPlayerSongDisksInventoryOutList1Item(r: PacketReader): PlayerSongDisksInventoryOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return PlayerSongDisksInventoryOut.List1Item(vInt1, vInt2)
    }

    private fun readPlayerSongDisksInventoryOut(r: PacketReader): PlayerSongDisksInventoryOut {
        val vList1 = List(r.countInt(8)) { readPlayerSongDisksInventoryOutList1Item(r) }
        return PlayerSongDisksInventoryOut(vList1)
    }

    private fun writePlayerSongDisksInventoryOutList1Item(w: PacketWriter, m: PlayerSongDisksInventoryOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writePlayerSongDisksInventoryOut(w: PacketWriter, m: PlayerSongDisksInventoryOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writePlayerSongDisksInventoryOutList1Item(w, it) }
    }
}
