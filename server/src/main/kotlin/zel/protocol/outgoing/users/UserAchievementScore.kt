package zel.protocol.outgoing.users

import zel.game.players.Player
import zel.protocol.generated.AchievementsScoreOut
import zel.protocol.generated.AchievementsScoreOutCodec
import zel.protocol.outgoing.OutgoingMessage

class UserAchievementScore(
    val player: Player,
) : OutgoingMessage<AchievementsScoreOut>(AchievementsScoreOutCodec) {
    override fun message() = AchievementsScoreOut(player.stats.tally.achievementScore)
}
