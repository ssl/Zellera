package zel.protocol.generated

data class RoomDimmerGetPresetsIn(
    val int1: Int,
)

object RoomDimmerGetPresetsInCodec : IncomingCodec<RoomDimmerGetPresetsIn> {
    override val name = "RoomDimmerGetPresetsIn"

    override fun read(r: PacketReader): RoomDimmerGetPresetsIn = readRoomDimmerGetPresetsIn(r)

    override fun write(w: PacketWriter, m: RoomDimmerGetPresetsIn) = writeRoomDimmerGetPresetsIn(w, m)

    private fun readRoomDimmerGetPresetsIn(r: PacketReader): RoomDimmerGetPresetsIn {
        val vInt1 = r.int()
        return RoomDimmerGetPresetsIn(vInt1)
    }

    private fun writeRoomDimmerGetPresetsIn(w: PacketWriter, m: RoomDimmerGetPresetsIn) {
        w.int(m.int1)
    }
}
