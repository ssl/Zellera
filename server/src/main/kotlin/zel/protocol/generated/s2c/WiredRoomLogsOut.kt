package zel.protocol.generated

data class WiredRoomLogsOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val list1: List<List1Item>,
    val boolean1: Boolean,
    val rest: ByteArray,
) {
    data class List1Item(
        val long1: Long,
        val byte1: Byte,
        val byte2: Byte,
        val string1: String,
        val long2: Long,
        val string2: String,
    )
}

object WiredRoomLogsOutCodec : OutgoingCodec<WiredRoomLogsOut> {
    override val name = "WiredRoomLogsOut"

    override fun read(r: PacketReader): WiredRoomLogsOut = readWiredRoomLogsOut(r)

    override fun write(w: PacketWriter, m: WiredRoomLogsOut) = writeWiredRoomLogsOut(w, m)

    private fun readWiredRoomLogsOutList1Item(r: PacketReader): WiredRoomLogsOut.List1Item {
        val vLong1 = r.long()
        val vByte1 = r.byte()
        val vByte2 = r.byte()
        val vString1 = r.string()
        val vLong2 = r.long()
        val vString2 = r.string()
        return WiredRoomLogsOut.List1Item(vLong1, vByte1, vByte2, vString1, vLong2, vString2)
    }

    private fun readWiredRoomLogsOut(r: PacketReader): WiredRoomLogsOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vList1 = List(r.countInt(22)) { readWiredRoomLogsOutList1Item(r) }
        val vBoolean1 = r.boolean()
        val vRest = r.rest()
        return WiredRoomLogsOut(vInt1, vInt2, vInt3, vList1, vBoolean1, vRest)
    }

    private fun writeWiredRoomLogsOutList1Item(w: PacketWriter, m: WiredRoomLogsOut.List1Item) {
        w.long(m.long1)
        w.byte(m.byte1)
        w.byte(m.byte2)
        w.string(m.string1)
        w.long(m.long2)
        w.string(m.string2)
    }

    private fun writeWiredRoomLogsOut(w: PacketWriter, m: WiredRoomLogsOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.countInt(m.list1.size)
        m.list1.forEach { writeWiredRoomLogsOutList1Item(w, it) }
        w.boolean(m.boolean1)
        w.bytes(m.rest)
    }
}
