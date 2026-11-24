package zel.protocol.generated

data class GetResolutionAchievementsIn(
    val itemId: Int,
    val achievementId: Int,
)

object GetResolutionAchievementsInCodec : IncomingCodec<GetResolutionAchievementsIn> {
    override val name = "GetResolutionAchievementsIn"

    override fun read(r: PacketReader): GetResolutionAchievementsIn = readGetResolutionAchievementsIn(r)

    override fun write(w: PacketWriter, m: GetResolutionAchievementsIn) = writeGetResolutionAchievementsIn(w, m)

    private fun readGetResolutionAchievementsIn(r: PacketReader): GetResolutionAchievementsIn {
        val vItemId = r.int()
        val vAchievementId = r.int()
        return GetResolutionAchievementsIn(vItemId, vAchievementId)
    }

    private fun writeGetResolutionAchievementsIn(w: PacketWriter, m: GetResolutionAchievementsIn) {
        w.int(m.itemId)
        w.int(m.achievementId)
    }
}
