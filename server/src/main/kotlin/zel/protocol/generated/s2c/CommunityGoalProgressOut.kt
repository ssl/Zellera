package zel.protocol.generated

data class CommunityGoalProgressOut(
    val hasGoalExpired: Boolean,
    val personalContributionScore: Int,
    val personalContributionRank: Int,
    val communityTotalScore: Int,
    val communityHighestAchievedLevel: Int,
    val scoreRemainingUntilNextLevel: Int,
    val percentCompletionTowardsNextLevel: Int,
    val goalCode: String,
    val timeRemainingInSeconds: Int,
    val rewardUserLimits: List<Int>,
)

object CommunityGoalProgressOutCodec : OutgoingCodec<CommunityGoalProgressOut> {
    override val name = "CommunityGoalProgressOut"

    override fun read(r: PacketReader): CommunityGoalProgressOut = readCommunityGoalProgressOut(r)

    override fun write(w: PacketWriter, m: CommunityGoalProgressOut) = writeCommunityGoalProgressOut(w, m)

    private fun readCommunityGoalProgressOut(r: PacketReader): CommunityGoalProgressOut {
        val vHasGoalExpired = r.boolean()
        val vPersonalContributionScore = r.int()
        val vPersonalContributionRank = r.int()
        val vCommunityTotalScore = r.int()
        val vCommunityHighestAchievedLevel = r.int()
        val vScoreRemainingUntilNextLevel = r.int()
        val vPercentCompletionTowardsNextLevel = r.int()
        val vGoalCode = r.string()
        val vTimeRemainingInSeconds = r.int()
        val vRewardUserLimits = List(r.countInt(4)) { r.int() }
        return CommunityGoalProgressOut(vHasGoalExpired, vPersonalContributionScore, vPersonalContributionRank, vCommunityTotalScore, vCommunityHighestAchievedLevel, vScoreRemainingUntilNextLevel, vPercentCompletionTowardsNextLevel, vGoalCode, vTimeRemainingInSeconds, vRewardUserLimits)
    }

    private fun writeCommunityGoalProgressOut(w: PacketWriter, m: CommunityGoalProgressOut) {
        w.boolean(m.hasGoalExpired)
        w.int(m.personalContributionScore)
        w.int(m.personalContributionRank)
        w.int(m.communityTotalScore)
        w.int(m.communityHighestAchievedLevel)
        w.int(m.scoreRemainingUntilNextLevel)
        w.int(m.percentCompletionTowardsNextLevel)
        w.string(m.goalCode)
        w.int(m.timeRemainingInSeconds)
        w.countInt(m.rewardUserLimits.size)
        m.rewardUserLimits.forEach { w.int(it) }
    }
}
