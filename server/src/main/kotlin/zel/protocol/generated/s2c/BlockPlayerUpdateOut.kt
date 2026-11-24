package zel.protocol.generated

data class BlockPlayerUpdateOut(
    val int1: Int,
    val int2: Int,
)

object BlockPlayerUpdateOutCodec : OutgoingCodec<BlockPlayerUpdateOut> {
    override val name = "BlockPlayerUpdateOut"

    override fun read(r: PacketReader): BlockPlayerUpdateOut = readBlockPlayerUpdateOut(r)

    override fun write(w: PacketWriter, m: BlockPlayerUpdateOut) = writeBlockPlayerUpdateOut(w, m)

    private fun readBlockPlayerUpdateOut(r: PacketReader): BlockPlayerUpdateOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return BlockPlayerUpdateOut(vInt1, vInt2)
    }

    private fun writeBlockPlayerUpdateOut(w: PacketWriter, m: BlockPlayerUpdateOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
