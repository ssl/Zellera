package zel.protocol.generated

data class RoomPropertyOut(
    val string1: String,
    val string2: String,
)

object RoomPropertyOutCodec : OutgoingCodec<RoomPropertyOut> {
    override val name = "RoomPropertyOut"

    override fun read(r: PacketReader): RoomPropertyOut = readRoomPropertyOut(r)

    override fun write(w: PacketWriter, m: RoomPropertyOut) = writeRoomPropertyOut(w, m)

    private fun readRoomPropertyOut(r: PacketReader): RoomPropertyOut {
        val vString1 = r.string()
        val vString2 = r.string()
        return RoomPropertyOut(vString1, vString2)
    }

    private fun writeRoomPropertyOut(w: PacketWriter, m: RoomPropertyOut) {
        w.string(m.string1)
        w.string(m.string2)
    }
}
