package zel.protocol.generated

data class RoomAdErrorOut(
    val int1: Int,
    val string1: String,
)

object RoomAdErrorOutCodec : OutgoingCodec<RoomAdErrorOut> {
    override val name = "RoomAdErrorOut"

    override fun read(r: PacketReader): RoomAdErrorOut = readRoomAdErrorOut(r)

    override fun write(w: PacketWriter, m: RoomAdErrorOut) = writeRoomAdErrorOut(w, m)

    private fun readRoomAdErrorOut(r: PacketReader): RoomAdErrorOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        return RoomAdErrorOut(vInt1, vString1)
    }

    private fun writeRoomAdErrorOut(w: PacketWriter, m: RoomAdErrorOut) {
        w.int(m.int1)
        w.string(m.string1)
    }
}
