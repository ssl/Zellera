package zel.protocol.generated

data class RoomForwardOut(
    val int1: Int,
)

object RoomForwardOutCodec : OutgoingCodec<RoomForwardOut> {
    override val name = "RoomForwardOut"

    override fun read(r: PacketReader): RoomForwardOut = readRoomForwardOut(r)

    override fun write(w: PacketWriter, m: RoomForwardOut) = writeRoomForwardOut(w, m)

    private fun readRoomForwardOut(r: PacketReader): RoomForwardOut {
        val vInt1 = r.int()
        return RoomForwardOut(vInt1)
    }

    private fun writeRoomForwardOut(w: PacketWriter, m: RoomForwardOut) {
        w.int(m.int1)
    }
}
