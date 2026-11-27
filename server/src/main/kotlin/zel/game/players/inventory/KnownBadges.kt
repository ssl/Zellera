package zel.game.players.inventory

import zel.game.achievements.Achievement
import zel.game.permissions.SecurityLevel

/**
 * Which badge codes the game has, exactly as written: a rank's badge, the VIP badge, a level an achievement has, or a
 * badge the texts name. An `ACH_` code of an achievement the game has is judged by its levels alone, so
 * `ACH_RoomEntry21` does not exist when RoomEntry stops at 20; an `ACH_` code of no achievement here (a retired one)
 * exists when the texts name that exact code.
 */
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

    /** True for a level an achievement has, false for one it lacks, null when no achievement has that name. */
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
