package zel.protocol.generated

data class RoomReadyOut(
    val string1: String,
    val int1: Int,
)

object RoomReadyOutCodec : OutgoingCodec<RoomReadyOut> {
    override val name = "RoomReadyOut"

    override fun read(r: PacketReader): RoomReadyOut = readRoomReadyOut(r)

    override fun write(w: PacketWriter, m: RoomReadyOut) = writeRoomReadyOut(w, m)

    private fun readRoomReadyOut(r: PacketReader): RoomReadyOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        return RoomReadyOut(vString1, vInt1)
    }

    private fun writeRoomReadyOut(w: PacketWriter, m: RoomReadyOut) {
        w.string(m.string1)
        w.int(m.int1)
    }
}
