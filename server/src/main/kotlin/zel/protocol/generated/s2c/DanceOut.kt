package zel.protocol.generated

data class DanceOut(
    val int1: Int,
    val int2: Int,
)

object DanceOutCodec : OutgoingCodec<DanceOut> {
    override val name = "DanceOut"

    override fun read(r: PacketReader): DanceOut = readDanceOut(r)

    override fun write(w: PacketWriter, m: DanceOut) = writeDanceOut(w, m)

    private fun readDanceOut(r: PacketReader): DanceOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return DanceOut(vInt1, vInt2)
    }

    private fun writeDanceOut(w: PacketWriter, m: DanceOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
