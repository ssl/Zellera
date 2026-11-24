package zel.protocol.generated

data class AchievementOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val int7: Int,
    val boolean1: Boolean,
    val string2: String,
    val string3: String,
    val int8: Int,
    val int9: Int,
    val short1: Short,
)

object AchievementOutCodec : OutgoingCodec<AchievementOut> {
    override val name = "AchievementOut"

    override fun read(r: PacketReader): AchievementOut = readAchievementOut(r)

    override fun write(w: PacketWriter, m: AchievementOut) = writeAchievementOut(w, m)

    private fun readAchievementOut(r: PacketReader): AchievementOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vBoolean1 = r.boolean()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt8 = r.int()
        val vInt9 = r.int()
        val vShort1 = r.short()
        return AchievementOut(vInt1, vInt2, vString1, vInt3, vInt4, vInt5, vInt6, vInt7, vBoolean1, vString2, vString3, vInt8, vInt9, vShort1)
    }

    private fun writeAchievementOut(w: PacketWriter, m: AchievementOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.boolean(m.boolean1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int8)
        w.int(m.int9)
        w.short(m.short1)
    }
}
