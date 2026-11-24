package zel.protocol.generated

data class AchievementsScoreOut(
    val int1: Int,
)

object AchievementsScoreOutCodec : OutgoingCodec<AchievementsScoreOut> {
    override val name = "AchievementsScoreOut"

    override fun read(r: PacketReader): AchievementsScoreOut = readAchievementsScoreOut(r)

    override fun write(w: PacketWriter, m: AchievementsScoreOut) = writeAchievementsScoreOut(w, m)

    private fun readAchievementsScoreOut(r: PacketReader): AchievementsScoreOut {
        val vInt1 = r.int()
        return AchievementsScoreOut(vInt1)
    }

    private fun writeAchievementsScoreOut(w: PacketWriter, m: AchievementsScoreOut) {
        w.int(m.int1)
    }
}
