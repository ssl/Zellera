package zel.game.achievements

import zel.content.ClientStrings
import zel.game.players.Player
import zel.util.SECONDS_PER_MINUTE
import java.util.concurrent.ConcurrentHashMap

/**
 * Online time: whole minutes are credited once five minutes have passed since the last credit. [named]: the achievement
 * a client profile string names.
 */
class GamePresenceAchievement(
    private val achievements: AchievementProgress,
    private val named: (String) -> Achievement? = { null },
) {
    private val clocks = ConcurrentHashMap<Int, Int>()

    fun minute(
        player: Player,
        now: Int,
    ) {
        val playerId = player.profile.id
        val since = clocks.putIfAbsent(playerId, now) ?: return
        val minutes = (now - since) / SECONDS_PER_MINUTE
        if (minutes < CREDIT_EVERY_MINUTES) return
        clocks[playerId] = since + minutes * SECONDS_PER_MINUTE
        val achievement = ClientStrings[ClientStrings.ONLINE_TIME_ACHIEVEMENT]?.let(named)
        achievements.progress(player, achievement, minutes)
    }

    fun keepOnly(present: Collection<Int>) {
        val keep = present.toSet()
        clocks.keys.removeIf { it !in keep }
    }

    private companion object {
        const val CREDIT_EVERY_MINUTES = 5
    }
}
