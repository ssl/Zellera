package zel.protocol.generated

data class AreaHideOut(
    val int1: Int,
    val boolean1: Boolean,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val boolean2: Boolean,
)

object AreaHideOutCodec : OutgoingCodec<AreaHideOut> {
    override val name = "AreaHideOut"

    override fun read(r: PacketReader): AreaHideOut = readAreaHideOut(r)

    override fun write(w: PacketWriter, m: AreaHideOut) = writeAreaHideOut(w, m)

    private fun readAreaHideOut(r: PacketReader): AreaHideOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vBoolean2 = r.boolean()
        return AreaHideOut(vInt1, vBoolean1, vInt2, vInt3, vInt4, vInt5, vBoolean2)
    }

    private fun writeAreaHideOut(w: PacketWriter, m: AreaHideOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.boolean(m.boolean2)
    }
}
