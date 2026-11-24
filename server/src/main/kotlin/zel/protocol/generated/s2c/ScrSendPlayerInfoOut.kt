package zel.protocol.generated

data class ScrSendPlayerInfoOut(
    val string1: String,
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val boolean1: Boolean,
    val boolean2: Boolean,
    val int5: Int,
    val int6: Int,
    val int7: Int,
    val trailing1: Int?,
)

object ScrSendPlayerInfoOutCodec : OutgoingCodec<ScrSendPlayerInfoOut> {
    override val name = "ScrSendPlayerInfoOut"

    override fun read(r: PacketReader): ScrSendPlayerInfoOut = readScrSendPlayerInfoOut(r)

    override fun write(w: PacketWriter, m: ScrSendPlayerInfoOut) = writeScrSendPlayerInfoOut(w, m)

    private fun readScrSendPlayerInfoOut(r: PacketReader): ScrSendPlayerInfoOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vTrailing1 = if (r.remaining > 0) r.int() else null
        return ScrSendPlayerInfoOut(vString1, vInt1, vInt2, vInt3, vInt4, vBoolean1, vBoolean2, vInt5, vInt6, vInt7, vTrailing1)
    }

    private fun writeScrSendPlayerInfoOut(w: PacketWriter, m: ScrSendPlayerInfoOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        m.trailing1?.let { w.int(it) }
    }
}
