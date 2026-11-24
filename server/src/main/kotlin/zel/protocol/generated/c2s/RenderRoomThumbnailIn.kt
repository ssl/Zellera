package zel.protocol.generated

data class RenderRoomThumbnailIn(
    val length: Int,
    val frame: ByteArray,
)

object RenderRoomThumbnailInCodec : IncomingCodec<RenderRoomThumbnailIn> {
    override val name = "RenderRoomThumbnailIn"

    override fun read(r: PacketReader): RenderRoomThumbnailIn = readRenderRoomThumbnailIn(r)

    override fun write(w: PacketWriter, m: RenderRoomThumbnailIn) = writeRenderRoomThumbnailIn(w, m)

    private fun readRenderRoomThumbnailIn(r: PacketReader): RenderRoomThumbnailIn {
        val vLength = r.int()
        val vFrame = r.rest()
        return RenderRoomThumbnailIn(vLength, vFrame)
    }

    private fun writeRenderRoomThumbnailIn(w: PacketWriter, m: RenderRoomThumbnailIn) {
        w.int(m.length)
        w.bytes(m.frame)
    }
}
