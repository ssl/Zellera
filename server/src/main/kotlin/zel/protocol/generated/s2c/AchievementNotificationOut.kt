package zel.protocol.generated

data class AchievementNotificationOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val string1: String,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val int7: Int,
    val int8: Int,
    val string2: String,
    val string3: String,
    val boolean1: Boolean,
    val int9: Int,
    val int10: Int,
)

object AchievementNotificationOutCodec : OutgoingCodec<AchievementNotificationOut> {
    override val name = "AchievementNotificationOut"

    override fun read(r: PacketReader): AchievementNotificationOut = readAchievementNotificationOut(r)

    override fun write(w: PacketWriter, m: AchievementNotificationOut) = writeAchievementNotificationOut(w, m)

    private fun readAchievementNotificationOut(r: PacketReader): AchievementNotificationOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vString2 = r.string()
        val vString3 = r.string()
        val vBoolean1 = r.boolean()
        val vInt9 = r.int()
        val vInt10 = r.int()
        return AchievementNotificationOut(vInt1, vInt2, vInt3, vString1, vInt4, vInt5, vInt6, vInt7, vInt8, vString2, vString3, vBoolean1, vInt9, vInt10)
    }

    private fun writeAchievementNotificationOut(w: PacketWriter, m: AchievementNotificationOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.string(m.string2)
        w.string(m.string3)
        w.boolean(m.boolean1)
        w.int(m.int9)
        w.int(m.int10)
    }
}
