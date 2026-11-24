package zel.protocol.generated

data class RoomDimmerChangeStateIn(
    val int1: Int,
)

object RoomDimmerChangeStateInCodec : IncomingCodec<RoomDimmerChangeStateIn> {
    override val name = "RoomDimmerChangeStateIn"

    override fun read(r: PacketReader): RoomDimmerChangeStateIn = readRoomDimmerChangeStateIn(r)

    override fun write(w: PacketWriter, m: RoomDimmerChangeStateIn) = writeRoomDimmerChangeStateIn(w, m)

    private fun readRoomDimmerChangeStateIn(r: PacketReader): RoomDimmerChangeStateIn {
        val vInt1 = r.int()
        return RoomDimmerChangeStateIn(vInt1)
    }

    private fun writeRoomDimmerChangeStateIn(w: PacketWriter, m: RoomDimmerChangeStateIn) {
        w.int(m.int1)
    }
}
