package zel.protocol.generated

data class ForumDataOut(
    val int1: Int,
    val string1: String,
    val string2: String,
    val string3: String,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val int7: Int,
    val string4: String,
    val int8: Int,
    val int9: Int,
    val int10: Int,
    val int11: Int,
    val int12: Int,
    val string5: String,
    val string6: String,
    val string7: String,
    val string8: String,
    val string9: String,
    val boolean1: Boolean,
    val boolean2: Boolean,
)

object ForumDataOutCodec : OutgoingCodec<ForumDataOut> {
    override val name = "ForumDataOut"

    override fun read(r: PacketReader): ForumDataOut = readForumDataOut(r)

    override fun write(w: PacketWriter, m: ForumDataOut) = writeForumDataOut(w, m)

    private fun readForumDataOut(r: PacketReader): ForumDataOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vString4 = r.string()
        val vInt8 = r.int()
        val vInt9 = r.int()
        val vInt10 = r.int()
        val vInt11 = r.int()
        val vInt12 = r.int()
        val vString5 = r.string()
        val vString6 = r.string()
        val vString7 = r.string()
        val vString8 = r.string()
        val vString9 = r.string()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        return ForumDataOut(vInt1, vString1, vString2, vString3, vInt2, vInt3, vInt4, vInt5, vInt6, vInt7, vString4, vInt8, vInt9, vInt10, vInt11, vInt12, vString5, vString6, vString7, vString8, vString9, vBoolean1, vBoolean2)
    }

    private fun writeForumDataOut(w: PacketWriter, m: ForumDataOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.string(m.string4)
        w.int(m.int8)
        w.int(m.int9)
        w.int(m.int10)
        w.int(m.int11)
        w.int(m.int12)
        w.string(m.string5)
        w.string(m.string6)
        w.string(m.string7)
        w.string(m.string8)
        w.string(m.string9)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
    }
}
