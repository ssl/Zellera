package zel.protocol.generated

data class PerformanceLogIn(
    val int1: Int,
    val string1: String,
    val string2: String,
    val string3: String,
    val string4: String,
    val boolean1: Boolean,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val int6: Int,
)

object PerformanceLogInCodec : IncomingCodec<PerformanceLogIn> {
    override val name = "PerformanceLogIn"

    override fun read(r: PacketReader): PerformanceLogIn = readPerformanceLogIn(r)

    override fun write(w: PacketWriter, m: PerformanceLogIn) = writePerformanceLogIn(w, m)

    private fun readPerformanceLogIn(r: PacketReader): PerformanceLogIn {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vString4 = r.string()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        return PerformanceLogIn(vInt1, vString1, vString2, vString3, vString4, vBoolean1, vInt2, vInt3, vInt4, vInt5, vInt6)
    }

    private fun writePerformanceLogIn(w: PacketWriter, m: PerformanceLogIn) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.string(m.string4)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
    }
}
