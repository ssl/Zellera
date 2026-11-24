package zel.protocol.generated

data class UpdateRoomThumbnailIn(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
)

object UpdateRoomThumbnailInCodec : IncomingCodec<UpdateRoomThumbnailIn> {
    override val name = "UpdateRoomThumbnailIn"

    override fun read(r: PacketReader): UpdateRoomThumbnailIn = readUpdateRoomThumbnailIn(r)

    override fun write(w: PacketWriter, m: UpdateRoomThumbnailIn) = writeUpdateRoomThumbnailIn(w, m)

    private fun readUpdateRoomThumbnailIn(r: PacketReader): UpdateRoomThumbnailIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        return UpdateRoomThumbnailIn(vInt1, vInt2, vInt3, vInt4)
    }

    private fun writeUpdateRoomThumbnailIn(w: PacketWriter, m: UpdateRoomThumbnailIn) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
    }
}
