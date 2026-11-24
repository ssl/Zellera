package zel.protocol.generated

data class RoomQueueStatusOut(
    val int1: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val int1: Int,
        val list1: List<List1Item>,
    ) {
        data class List1Item(
            val string1: String,
            val int1: Int,
        )
    }
}

object RoomQueueStatusOutCodec : OutgoingCodec<RoomQueueStatusOut> {
    override val name = "RoomQueueStatusOut"

    override fun read(r: PacketReader): RoomQueueStatusOut = readRoomQueueStatusOut(r)

    override fun write(w: PacketWriter, m: RoomQueueStatusOut) = writeRoomQueueStatusOut(w, m)

    private fun readRoomQueueStatusOutList1ItemList1Item(r: PacketReader): RoomQueueStatusOut.List1Item.List1Item {
        val vString1 = r.string()
        val vInt1 = r.int()
        return RoomQueueStatusOut.List1Item.List1Item(vString1, vInt1)
    }

    private fun readRoomQueueStatusOutList1Item(r: PacketReader): RoomQueueStatusOut.List1Item {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vList1 = List(r.countInt(6)) { readRoomQueueStatusOutList1ItemList1Item(r) }
        return RoomQueueStatusOut.List1Item(vString1, vInt1, vList1)
    }

    private fun readRoomQueueStatusOut(r: PacketReader): RoomQueueStatusOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(10)) { readRoomQueueStatusOutList1Item(r) }
        return RoomQueueStatusOut(vInt1, vList1)
    }

    private fun writeRoomQueueStatusOutList1ItemList1Item(w: PacketWriter, m: RoomQueueStatusOut.List1Item.List1Item) {
        w.string(m.string1)
        w.int(m.int1)
    }

    private fun writeRoomQueueStatusOutList1Item(w: PacketWriter, m: RoomQueueStatusOut.List1Item) {
        w.string(m.string1)
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeRoomQueueStatusOutList1ItemList1Item(w, it) }
    }

    private fun writeRoomQueueStatusOut(w: PacketWriter, m: RoomQueueStatusOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeRoomQueueStatusOutList1Item(w, it) }
    }
}
