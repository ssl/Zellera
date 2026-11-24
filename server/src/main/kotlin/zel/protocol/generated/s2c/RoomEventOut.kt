package zel.protocol.generated

data class RoomEventOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val int3: Int,
    val int4: Int,
    val string2: String,
    val string3: String,
    val int5: Int,
    val int6: Int,
    val int7: Int,
)

object RoomEventOutCodec : OutgoingCodec<RoomEventOut> {
    override val name = "RoomEventOut"

    override fun read(r: PacketReader): RoomEventOut = readRoomEventOut(r)

    override fun write(w: PacketWriter, m: RoomEventOut) = writeRoomEventOut(w, m)

    private fun readRoomEventOut(r: PacketReader): RoomEventOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        return RoomEventOut(vInt1, vInt2, vString1, vInt3, vInt4, vString2, vString3, vInt5, vInt6, vInt7)
    }

    private fun writeRoomEventOut(w: PacketWriter, m: RoomEventOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.int(m.int4)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
    }
}
