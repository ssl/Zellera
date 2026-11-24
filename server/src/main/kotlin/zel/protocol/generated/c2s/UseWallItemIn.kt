package zel.protocol.generated

data class UseWallItemIn(
    val itemId: Int,
    val state: Int,
)

object UseWallItemInCodec : IncomingCodec<UseWallItemIn> {
    override val name = "UseWallItemIn"

    override fun read(r: PacketReader): UseWallItemIn = readUseWallItemIn(r)

    override fun write(w: PacketWriter, m: UseWallItemIn) = writeUseWallItemIn(w, m)

    private fun readUseWallItemIn(r: PacketReader): UseWallItemIn {
        val vItemId = r.int()
        val vState = r.int()
        return UseWallItemIn(vItemId, vState)
    }

    private fun writeUseWallItemIn(w: PacketWriter, m: UseWallItemIn) {
        w.int(m.itemId)
        w.int(m.state)
    }
}
