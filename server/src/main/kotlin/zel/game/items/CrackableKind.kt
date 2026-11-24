package zel.game.items

import zel.game.items.mysterybox.WeightedDraw
import zel.game.players.subscriptions.Subscription
import java.sql.ResultSet
import kotlin.random.Random

data class CrackableKind(
    val hitsNeeded: Int,
    val hitAchievement: String,
    val crackedAchievement: String,
    val requiredEffect: Int,
    val membershipDays: Int,
    val membership: String?,
    val prizes: List<Prize>,
) {
    /** Definition 0 is the chance of nothing. */
    data class Prize(
        val definitionId: Int,
        val chance: Int,
    )

    private val tickets = WeightedDraw(prizes.map { it.definitionId to it.chance })

    val givesMembership: Boolean get() = membership != null && membershipDays > 0

    /** The drawn definition, 0 for nothing. */
    fun draw(dice: Random = Random.Default): Int = tickets.draw(dice) ?: NOTHING

    companion object {
        private const val NOTHING = 0

        private val MEMBERSHIPS = mapOf("club" to Subscription.CLUB, "build_pass" to Subscription.BUILD_PASS)

        fun read(
            row: ResultSet,
            prizes: List<Prize>,
        ): CrackableKind {
            val nothingChance = row.getInt("no_prize_chance")
            return CrackableKind(
                hitsNeeded = row.getInt("hits_needed"),
                hitAchievement = row.getString("hit_achievement").orEmpty(),
                crackedAchievement = row.getString("cracked_achievement").orEmpty(),
                requiredEffect = row.getInt("required_effect"),
                membershipDays = row.getInt("subscription_days"),
                membership = MEMBERSHIPS[row.getString("subscription_kind")],
                prizes = if (nothingChance > 0) prizes + Prize(NOTHING, nothingChance) else prizes,
            )
        }
    }
}
