package zel.protocol.generated

data class RoomRatingOut(
    val int1: Int,
    val boolean1: Boolean,
)

object RoomRatingOutCodec : OutgoingCodec<RoomRatingOut> {
    override val name = "RoomRatingOut"

    override fun read(r: PacketReader): RoomRatingOut = readRoomRatingOut(r)

    override fun write(w: PacketWriter, m: RoomRatingOut) = writeRoomRatingOut(w, m)

    private fun readRoomRatingOut(r: PacketReader): RoomRatingOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return RoomRatingOut(vInt1, vBoolean1)
    }

    private fun writeRoomRatingOut(w: PacketWriter, m: RoomRatingOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
    }
}
