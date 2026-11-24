package zel.game.messenger

import zel.game.achievements.Achievement
import zel.game.achievements.Achievements
import zel.game.achievements.GameEvents
import zel.game.players.Player

class FriendListAchievement(
    private val achievements: Achievements,
) {
    fun friendCountChanged(player: Player) {
        for (achievement in achievements()) {
            val behind = behind(player, achievement)
            if (behind > 0) achievement.progress(player, behind)
        }
    }

    /** After MessengerInit the achievement event is sent even when nothing moved. */
    fun listSent(player: Player) {
        if (player.client == null) return
        for (achievement in achievements()) {
            val behind = behind(player, achievement)
            if (behind > 0 && !achievement.isFinishedAt(player.stats.progressOf(achievement))) {
                achievement.progress(player, behind)
            } else {
                achievement.announce(player)
            }
        }
    }

    private fun achievements(): List<Achievement> = achievements.forEvent(GameEvents.PLAYER_FRIENDS)

    private fun behind(
        player: Player,
        achievement: Achievement,
    ) = progressBehind(player.stats.progressOf(achievement), player.messenger.friends.size)

    companion object {
        fun progressBehind(
            progress: Int,
            friendCount: Int,
        ): Int = friendCount - maxOf(0, progress)
    }
}
