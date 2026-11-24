package zel.protocol.generated

data class RoomUseIconOut(
    val int1: Int,
    val int2: Int,
)

object RoomUseIconOutCodec : OutgoingCodec<RoomUseIconOut> {
    override val name = "RoomUseIconOut"

    override fun read(r: PacketReader): RoomUseIconOut = readRoomUseIconOut(r)

    override fun write(w: PacketWriter, m: RoomUseIconOut) = writeRoomUseIconOut(w, m)

    private fun readRoomUseIconOut(r: PacketReader): RoomUseIconOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return RoomUseIconOut(vInt1, vInt2)
    }

    private fun writeRoomUseIconOut(w: PacketWriter, m: RoomUseIconOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
