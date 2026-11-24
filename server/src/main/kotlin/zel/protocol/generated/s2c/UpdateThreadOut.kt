package zel.protocol.generated

data class UpdateThreadOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val string1: String,
    val string2: String,
    val boolean1: Boolean,
    val boolean2: Boolean,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val int7: Int,
    val int8: Int,
    val string3: String,
    val int9: Int,
    val byte1: Byte,
    val int10: Int,
    val string4: String,
    val int11: Int,
)

object UpdateThreadOutCodec : OutgoingCodec<UpdateThreadOut> {
    override val name = "UpdateThreadOut"

    override fun read(r: PacketReader): UpdateThreadOut = readUpdateThreadOut(r)

    override fun write(w: PacketWriter, m: UpdateThreadOut) = writeUpdateThreadOut(w, m)

    private fun readUpdateThreadOut(r: PacketReader): UpdateThreadOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vString3 = r.string()
        val vInt9 = r.int()
        val vByte1 = r.byte()
        val vInt10 = r.int()
        val vString4 = r.string()
        val vInt11 = r.int()
        return UpdateThreadOut(vInt1, vInt2, vInt3, vString1, vString2, vBoolean1, vBoolean2, vInt4, vInt5, vInt6, vInt7, vInt8, vString3, vInt9, vByte1, vInt10, vString4, vInt11)
    }

    private fun writeUpdateThreadOut(w: PacketWriter, m: UpdateThreadOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.string(m.string2)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.string(m.string3)
        w.int(m.int9)
        w.byte(m.byte1)
        w.int(m.int10)
        w.string(m.string4)
        w.int(m.int11)
    }
}
