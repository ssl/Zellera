package zel.protocol.generated

data class RoomInfoUpdatedOut(
    val int1: Int,
)

object RoomInfoUpdatedOutCodec : OutgoingCodec<RoomInfoUpdatedOut> {
    override val name = "RoomInfoUpdatedOut"

    override fun read(r: PacketReader): RoomInfoUpdatedOut = readRoomInfoUpdatedOut(r)

    override fun write(w: PacketWriter, m: RoomInfoUpdatedOut) = writeRoomInfoUpdatedOut(w, m)

    private fun readRoomInfoUpdatedOut(r: PacketReader): RoomInfoUpdatedOut {
        val vInt1 = r.int()
        return RoomInfoUpdatedOut(vInt1)
    }

    private fun writeRoomInfoUpdatedOut(w: PacketWriter, m: RoomInfoUpdatedOut) {
        w.int(m.int1)
    }
}
