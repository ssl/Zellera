package zel.protocol.generated

data class MoveWallItemIn(
    val itemId: Int,
    val wallPosition: String,
)

object MoveWallItemInCodec : IncomingCodec<MoveWallItemIn> {
    override val name = "MoveWallItemIn"

    override fun read(r: PacketReader): MoveWallItemIn = readMoveWallItemIn(r)

    override fun write(w: PacketWriter, m: MoveWallItemIn) = writeMoveWallItemIn(w, m)

    private fun readMoveWallItemIn(r: PacketReader): MoveWallItemIn {
        val vItemId = r.int()
        val vWallPosition = r.string()
        return MoveWallItemIn(vItemId, vWallPosition)
    }

    private fun writeMoveWallItemIn(w: PacketWriter, m: MoveWallItemIn) {
        w.int(m.itemId)
        w.string(m.wallPosition)
    }
}
