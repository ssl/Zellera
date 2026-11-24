package zel.protocol.generated

data class RenderRoomIn(
    val length: Int,
    val frame: ByteArray,
)

object RenderRoomInCodec : IncomingCodec<RenderRoomIn> {
    override val name = "RenderRoomIn"

    override fun read(r: PacketReader): RenderRoomIn = readRenderRoomIn(r)

    override fun write(w: PacketWriter, m: RenderRoomIn) = writeRenderRoomIn(w, m)

    private fun readRenderRoomIn(r: PacketReader): RenderRoomIn {
        val vLength = r.int()
        val vFrame = r.rest()
        return RenderRoomIn(vLength, vFrame)
    }

    private fun writeRenderRoomIn(w: PacketWriter, m: RenderRoomIn) {
        w.int(m.length)
        w.bytes(m.frame)
    }
}
