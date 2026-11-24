package zel.protocol.generated

data class RoomOccupiedTilesOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
    )
}

object RoomOccupiedTilesOutCodec : OutgoingCodec<RoomOccupiedTilesOut> {
    override val name = "RoomOccupiedTilesOut"

    override fun read(r: PacketReader): RoomOccupiedTilesOut = readRoomOccupiedTilesOut(r)

    override fun write(w: PacketWriter, m: RoomOccupiedTilesOut) = writeRoomOccupiedTilesOut(w, m)

    private fun readRoomOccupiedTilesOutList1Item(r: PacketReader): RoomOccupiedTilesOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return RoomOccupiedTilesOut.List1Item(vInt1, vInt2)
    }

    private fun readRoomOccupiedTilesOut(r: PacketReader): RoomOccupiedTilesOut {
        val vList1 = List(r.countInt(8)) { readRoomOccupiedTilesOutList1Item(r) }
        return RoomOccupiedTilesOut(vList1)
    }

    private fun writeRoomOccupiedTilesOutList1Item(w: PacketWriter, m: RoomOccupiedTilesOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writeRoomOccupiedTilesOut(w: PacketWriter, m: RoomOccupiedTilesOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeRoomOccupiedTilesOutList1Item(w, it) }
    }
}
