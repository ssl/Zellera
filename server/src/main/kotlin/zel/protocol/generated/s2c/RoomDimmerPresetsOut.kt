package zel.protocol.generated

data class RoomDimmerPresetsOut(
    val int1: Int,
    val list1: List<List1Item>,
    val boolean1: Boolean,
    val int2: Int,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val string1: String,
        val int3: Int,
    )
}

object RoomDimmerPresetsOutCodec : OutgoingCodec<RoomDimmerPresetsOut> {
    override val name = "RoomDimmerPresetsOut"

    override fun read(r: PacketReader): RoomDimmerPresetsOut = readRoomDimmerPresetsOut(r)

    override fun write(w: PacketWriter, m: RoomDimmerPresetsOut) = writeRoomDimmerPresetsOut(w, m)

    private fun readRoomDimmerPresetsOutList1Item(r: PacketReader): RoomDimmerPresetsOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        return RoomDimmerPresetsOut.List1Item(vInt1, vInt2, vString1, vInt3)
    }

    private fun readRoomDimmerPresetsOut(r: PacketReader): RoomDimmerPresetsOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(14)) { readRoomDimmerPresetsOutList1Item(r) }
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        return RoomDimmerPresetsOut(vInt1, vList1, vBoolean1, vInt2)
    }

    private fun writeRoomDimmerPresetsOutList1Item(w: PacketWriter, m: RoomDimmerPresetsOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
    }

    private fun writeRoomDimmerPresetsOut(w: PacketWriter, m: RoomDimmerPresetsOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeRoomDimmerPresetsOutList1Item(w, it) }
        w.boolean(m.boolean1)
        w.int(m.int2)
    }
}
