package zel.protocol.generated

data class AchievementResolutionCompletedOut(
    val string1: String,
    val string2: String,
)

object AchievementResolutionCompletedOutCodec : OutgoingCodec<AchievementResolutionCompletedOut> {
    override val name = "AchievementResolutionCompletedOut"

    override fun read(r: PacketReader): AchievementResolutionCompletedOut = readAchievementResolutionCompletedOut(r)

    override fun write(w: PacketWriter, m: AchievementResolutionCompletedOut) = writeAchievementResolutionCompletedOut(w, m)

    private fun readAchievementResolutionCompletedOut(r: PacketReader): AchievementResolutionCompletedOut {
        val vString1 = r.string()
        val vString2 = r.string()
        return AchievementResolutionCompletedOut(vString1, vString2)
    }

    private fun writeAchievementResolutionCompletedOut(w: PacketWriter, m: AchievementResolutionCompletedOut) {
        w.string(m.string1)
        w.string(m.string2)
    }
}
