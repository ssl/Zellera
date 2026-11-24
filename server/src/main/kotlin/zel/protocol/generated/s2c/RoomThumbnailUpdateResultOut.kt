package zel.protocol.generated

data class RoomThumbnailUpdateResultOut(
    val int1: Int,
    val int2: Int,
)

object RoomThumbnailUpdateResultOutCodec : OutgoingCodec<RoomThumbnailUpdateResultOut> {
    override val name = "RoomThumbnailUpdateResultOut"

    override fun read(r: PacketReader): RoomThumbnailUpdateResultOut = readRoomThumbnailUpdateResultOut(r)

    override fun write(w: PacketWriter, m: RoomThumbnailUpdateResultOut) = writeRoomThumbnailUpdateResultOut(w, m)

    private fun readRoomThumbnailUpdateResultOut(r: PacketReader): RoomThumbnailUpdateResultOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return RoomThumbnailUpdateResultOut(vInt1, vInt2)
    }

    private fun writeRoomThumbnailUpdateResultOut(w: PacketWriter, m: RoomThumbnailUpdateResultOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
