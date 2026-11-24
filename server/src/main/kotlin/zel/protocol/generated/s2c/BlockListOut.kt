package zel.protocol.generated

data class BlockListOut(
    val list1: List<Int>,
)

object BlockListOutCodec : OutgoingCodec<BlockListOut> {
    override val name = "BlockListOut"

    override fun read(r: PacketReader): BlockListOut = readBlockListOut(r)

    override fun write(w: PacketWriter, m: BlockListOut) = writeBlockListOut(w, m)

    private fun readBlockListOut(r: PacketReader): BlockListOut {
        val vList1 = List(r.countInt(4)) { r.int() }
        return BlockListOut(vList1)
    }

    private fun writeBlockListOut(w: PacketWriter, m: BlockListOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }
}
