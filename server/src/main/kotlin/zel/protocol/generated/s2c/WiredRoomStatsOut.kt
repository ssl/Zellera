package zel.protocol.generated

data class WiredRoomStatsOut(
    val double1: Double,
    val double2: Double,
    val boolean1: Boolean,
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val int7: Int,
    val int8: Int,
    val int9: Int,
    val int10: Int,
)

object WiredRoomStatsOutCodec : OutgoingCodec<WiredRoomStatsOut> {
    override val name = "WiredRoomStatsOut"

    override fun read(r: PacketReader): WiredRoomStatsOut = readWiredRoomStatsOut(r)

    override fun write(w: PacketWriter, m: WiredRoomStatsOut) = writeWiredRoomStatsOut(w, m)

    private fun readWiredRoomStatsOut(r: PacketReader): WiredRoomStatsOut {
        val vDouble1 = r.double()
        val vDouble2 = r.double()
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vInt9 = r.int()
        val vInt10 = r.int()
        return WiredRoomStatsOut(vDouble1, vDouble2, vBoolean1, vInt1, vInt2, vInt3, vInt4, vInt5, vInt6, vInt7, vInt8, vInt9, vInt10)
    }

    private fun writeWiredRoomStatsOut(w: PacketWriter, m: WiredRoomStatsOut) {
        w.double(m.double1)
        w.double(m.double2)
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.int(m.int9)
        w.int(m.int10)
    }
}
