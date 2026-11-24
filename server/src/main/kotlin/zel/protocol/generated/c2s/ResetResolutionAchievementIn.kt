package zel.protocol.generated

data class ResetResolutionAchievementIn(
    val itemId: Int,
)

object ResetResolutionAchievementInCodec : IncomingCodec<ResetResolutionAchievementIn> {
    override val name = "ResetResolutionAchievementIn"

    override fun read(r: PacketReader): ResetResolutionAchievementIn = readResetResolutionAchievementIn(r)

    override fun write(w: PacketWriter, m: ResetResolutionAchievementIn) = writeResetResolutionAchievementIn(w, m)

    private fun readResetResolutionAchievementIn(r: PacketReader): ResetResolutionAchievementIn {
        val vItemId = r.int()
        return ResetResolutionAchievementIn(vItemId)
    }

    private fun writeResetResolutionAchievementIn(w: PacketWriter, m: ResetResolutionAchievementIn) {
        w.int(m.itemId)
    }
}
