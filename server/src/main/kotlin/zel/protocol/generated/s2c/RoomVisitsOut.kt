package zel.protocol.generated

data class RoomVisitsOut(
    val int1: Int,
    val string1: String,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val int2: Int,
        val int3: Int,
    )
}

object RoomVisitsOutCodec : OutgoingCodec<RoomVisitsOut> {
    override val name = "RoomVisitsOut"

    override fun read(r: PacketReader): RoomVisitsOut = readRoomVisitsOut(r)

    override fun write(w: PacketWriter, m: RoomVisitsOut) = writeRoomVisitsOut(w, m)

    private fun readRoomVisitsOutList1Item(r: PacketReader): RoomVisitsOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return RoomVisitsOut.List1Item(vInt1, vString1, vInt2, vInt3)
    }

    private fun readRoomVisitsOut(r: PacketReader): RoomVisitsOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vList1 = List(r.countInt(14)) { readRoomVisitsOutList1Item(r) }
        return RoomVisitsOut(vInt1, vString1, vList1)
    }

    private fun writeRoomVisitsOutList1Item(w: PacketWriter, m: RoomVisitsOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
    }

    private fun writeRoomVisitsOut(w: PacketWriter, m: RoomVisitsOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeRoomVisitsOutList1Item(w, it) }
    }
}
