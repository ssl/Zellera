package zel.protocol.generated

data class RecyclerStatusOut(
    val int1: Int,
    val int2: Int,
)

object RecyclerStatusOutCodec : OutgoingCodec<RecyclerStatusOut> {
    override val name = "RecyclerStatusOut"

    override fun read(r: PacketReader): RecyclerStatusOut = readRecyclerStatusOut(r)

    override fun write(w: PacketWriter, m: RecyclerStatusOut) = writeRecyclerStatusOut(w, m)

    private fun readRecyclerStatusOut(r: PacketReader): RecyclerStatusOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return RecyclerStatusOut(vInt1, vInt2)
    }

    private fun writeRecyclerStatusOut(w: PacketWriter, m: RecyclerStatusOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
