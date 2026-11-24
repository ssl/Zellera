package zel.protocol.generated

data class AchievementResolutionProgressOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val int3: Int,
    val int4: Int,
    val int5: Int,
)

object AchievementResolutionProgressOutCodec : OutgoingCodec<AchievementResolutionProgressOut> {
    override val name = "AchievementResolutionProgressOut"

    override fun read(r: PacketReader): AchievementResolutionProgressOut = readAchievementResolutionProgressOut(r)

    override fun write(w: PacketWriter, m: AchievementResolutionProgressOut) = writeAchievementResolutionProgressOut(w, m)

    private fun readAchievementResolutionProgressOut(r: PacketReader): AchievementResolutionProgressOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        return AchievementResolutionProgressOut(vInt1, vInt2, vString1, vInt3, vInt4, vInt5)
    }

    private fun writeAchievementResolutionProgressOut(w: PacketWriter, m: AchievementResolutionProgressOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
    }
}
