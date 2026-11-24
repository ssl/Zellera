package zel.protocol.generated

data class IconInfoOut(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val int6: Int,
)

object IconInfoOutCodec : OutgoingCodec<IconInfoOut> {
    override val name = "IconInfoOut"

    override fun read(r: PacketReader): IconInfoOut = readIconInfoOut(r)

    override fun write(w: PacketWriter, m: IconInfoOut) = writeIconInfoOut(w, m)

    private fun readIconInfoOut(r: PacketReader): IconInfoOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        return IconInfoOut(vInt1, vString1, vInt2, vInt3, vInt4, vInt5, vInt6)
    }

    private fun writeIconInfoOut(w: PacketWriter, m: IconInfoOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
    }
}
