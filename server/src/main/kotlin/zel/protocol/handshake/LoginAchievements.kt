package zel.protocol.handshake

import zel.game.achievements.Achievement
import zel.game.achievements.AchievementEvents
import zel.game.achievements.Achievements
import zel.game.achievements.GameEvents
import zel.game.players.Player

class LoginAchievements(
    private val achievements: Achievements,
) {
    fun progress(
        player: Player,
        now: Int,
    ) {
        val stats = player.stats
        val profile = player.profile
        stats.tally.loginStreak = LoginCalendar.streak(stats.tally.loginStreak, profile.lastOnlineAt, now)
        raiseTo(player, GameEvents.PLAYER_LOGIN_STREAK, stats.tally.loginStreak)
        raiseTo(player, GameEvents.PLAYER_REGISTRATION_DAYS, LoginCalendar.daysBetween(profile.createdAt, now))

        achievements.settleStanding(player)
        // The online time achievement moves on the five-minute clock only, never at login
        AchievementEvents.identity().atLogin(player)
    }

    private fun raiseTo(
        player: Player,
        event: String,
        target: Int,
    ) = achievements.forEvent(event).forEach { raiseTo(player, it, target) }

    private fun raiseTo(
        player: Player,
        achievement: Achievement,
        target: Int,
    ) {
        val behind = target - maxOf(0, player.stats.progressOf(achievement))
        if (behind > 0) achievement.progress(player, behind)
    }
}
