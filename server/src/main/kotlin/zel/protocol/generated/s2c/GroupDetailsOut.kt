package zel.protocol.generated

data class GroupDetailsOut(
    val int1: Int,
    val boolean1: Boolean,
    val int2: Int,
    val string1: String,
    val string2: String,
    val string3: String,
    val int3: Int,
    val string4: String,
    val int4: Int,
    val int5: Int,
    val boolean2: Boolean,
    val string5: String,
    val boolean3: Boolean,
    val boolean4: Boolean,
    val string6: String,
    val boolean5: Boolean,
    val boolean6: Boolean,
    val int6: Int,
    val boolean7: Boolean,
)

object GroupDetailsOutCodec : OutgoingCodec<GroupDetailsOut> {
    override val name = "GroupDetailsOut"

    override fun read(r: PacketReader): GroupDetailsOut = readGroupDetailsOut(r)

    override fun write(w: PacketWriter, m: GroupDetailsOut) = writeGroupDetailsOut(w, m)

    private fun readGroupDetailsOut(r: PacketReader): GroupDetailsOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt3 = r.int()
        val vString4 = r.string()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vBoolean2 = r.boolean()
        val vString5 = r.string()
        val vBoolean3 = r.boolean()
        val vBoolean4 = r.boolean()
        val vString6 = r.string()
        val vBoolean5 = r.boolean()
        val vBoolean6 = r.boolean()
        val vInt6 = r.int()
        val vBoolean7 = r.boolean()
        return GroupDetailsOut(vInt1, vBoolean1, vInt2, vString1, vString2, vString3, vInt3, vString4, vInt4, vInt5, vBoolean2, vString5, vBoolean3, vBoolean4, vString6, vBoolean5, vBoolean6, vInt6, vBoolean7)
    }

    private fun writeGroupDetailsOut(w: PacketWriter, m: GroupDetailsOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int3)
        w.string(m.string4)
        w.int(m.int4)
        w.int(m.int5)
        w.boolean(m.boolean2)
        w.string(m.string5)
        w.boolean(m.boolean3)
        w.boolean(m.boolean4)
        w.string(m.string6)
        w.boolean(m.boolean5)
        w.boolean(m.boolean6)
        w.int(m.int6)
        w.boolean(m.boolean7)
    }
}
