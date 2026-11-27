package zel.game.players.inventory

import zel.game.achievements.Achievement
import zel.game.permissions.SecurityLevel

class KnownBadges(
    private val achievements: () -> Collection<Achievement>,
    private val rankBadges: () -> Set<String>,
    private val named: (String) -> Boolean,
) {
    fun exists(code: String): Boolean =
        when {
            code == SecurityLevel.VIP_BADGE || code in rankBadges() -> true
            code.startsWith(ACHIEVEMENT_PREFIX) -> achievementLevel(code) ?: named(code)
            else -> named(code)
        }

    private fun achievementLevel(code: String): Boolean? {
        val all = achievements()
        if (all.any { achievement -> achievement.levels.keys.any { achievement.badgeCode(it) == code } }) return true
        val name = code.removePrefix(ACHIEVEMENT_PREFIX)
        val ofAnAchievement = all.any { name.startsWith(it.name) && name.substring(it.name.length).all(Char::isDigit) }
        return if (ofAnAchievement) false else null
    }

    companion object {
        private const val ACHIEVEMENT_PREFIX = "ACH_"

        fun missing(code: String): String = "The game has no badge called $code."
    }
}
