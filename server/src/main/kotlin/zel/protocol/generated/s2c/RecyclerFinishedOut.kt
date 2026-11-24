package zel.protocol.generated

data class RecyclerFinishedOut(
    val int1: Int,
    val int2: Int,
)

object RecyclerFinishedOutCodec : OutgoingCodec<RecyclerFinishedOut> {
    override val name = "RecyclerFinishedOut"

    override fun read(r: PacketReader): RecyclerFinishedOut = readRecyclerFinishedOut(r)

    override fun write(w: PacketWriter, m: RecyclerFinishedOut) = writeRecyclerFinishedOut(w, m)

    private fun readRecyclerFinishedOut(r: PacketReader): RecyclerFinishedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return RecyclerFinishedOut(vInt1, vInt2)
    }

    private fun writeRecyclerFinishedOut(w: PacketWriter, m: RecyclerFinishedOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
