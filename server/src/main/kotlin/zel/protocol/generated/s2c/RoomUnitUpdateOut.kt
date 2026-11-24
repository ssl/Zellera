package zel.protocol.generated

data class RoomUnitUpdateOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
        val string1: String,
        val int4: Int,
        val int5: Int,
        val int6: Int,
        val string2: String,
    )
}

object RoomUnitUpdateOutCodec : OutgoingCodec<RoomUnitUpdateOut> {
    override val name = "RoomUnitUpdateOut"

    override fun read(r: PacketReader): RoomUnitUpdateOut = readRoomUnitUpdateOut(r)

    override fun write(w: PacketWriter, m: RoomUnitUpdateOut) = writeRoomUnitUpdateOut(w, m)

    private fun readRoomUnitUpdateOutList1Item(r: PacketReader): RoomUnitUpdateOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vString2 = r.string()
        return RoomUnitUpdateOut.List1Item(vInt1, vInt2, vInt3, vString1, vInt4, vInt5, vInt6, vString2)
    }

    private fun readRoomUnitUpdateOut(r: PacketReader): RoomUnitUpdateOut {
        val vList1 = List(r.countInt(28)) { readRoomUnitUpdateOutList1Item(r) }
        return RoomUnitUpdateOut(vList1)
    }

    private fun writeRoomUnitUpdateOutList1Item(w: PacketWriter, m: RoomUnitUpdateOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.string(m.string2)
    }

    private fun writeRoomUnitUpdateOut(w: PacketWriter, m: RoomUnitUpdateOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeRoomUnitUpdateOutList1Item(w, it) }
    }
}
