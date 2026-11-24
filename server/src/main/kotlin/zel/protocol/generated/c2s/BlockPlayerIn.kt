package zel.protocol.generated

data class BlockPlayerIn(
    val userId: Int,
)

object BlockPlayerInCodec : IncomingCodec<BlockPlayerIn> {
    override val name = "BlockPlayerIn"

    override fun read(r: PacketReader): BlockPlayerIn = readBlockPlayerIn(r)

    override fun write(w: PacketWriter, m: BlockPlayerIn) = writeBlockPlayerIn(w, m)

    private fun readBlockPlayerIn(r: PacketReader): BlockPlayerIn {
        val vUserId = r.int()
        return BlockPlayerIn(vUserId)
    }

    private fun writeBlockPlayerIn(w: PacketWriter, m: BlockPlayerIn) {
        w.int(m.userId)
    }
}
