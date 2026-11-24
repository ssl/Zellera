package zel.game.communitygoals

import zel.core.Configuration
import zel.game.players.Player
import zel.game.players.Players
import java.util.function.IntSupplier

/** A blank badge setting gives no badge. */
class ConcurrentPlayersGoal(
    private val claims: GoalClaims,
    private val configuration: Configuration,
    private val players: Players,
    private val unixTime: IntSupplier,
) {
    enum class State(
        val code: Int,
    ) {
        OFF(0),
        COUNTING(1),
        REACHED(2),
        REWARDED(REWARDED_WIRE),
    }

    val target: Int get() = configuration.getInt(TARGET_KEY, 0)

    val online: Int get() = players.count

    fun stateFor(playerId: Int): State =
        when {
            target <= 0 -> State.OFF
            online < target -> State.COUNTING
            claims.rewarded(CLAIM_CODE, playerId) -> State.REWARDED
            else -> State.REACHED
        }

    fun claimReward(player: Player): Boolean {
        val playerId = player.profile.id
        if (stateFor(playerId) != State.REACHED) return false
        if (!claims.markRewarded(CLAIM_CODE, playerId, unixTime.asInt)) return false
        val badge = configuration.getValue(BADGE_KEY, DEFAULT_BADGE)
        if (badge.isNotBlank()) player.inventory.badges.award(badge)
        return true
    }

    private companion object {
        const val TARGET_KEY = "community_goal.concurrent_players.target"
        const val BADGE_KEY = "community_goal.concurrent_players.badge_code"
        const val DEFAULT_BADGE = "ConcurrentUsersReward"
        const val CLAIM_CODE = "concurrentusers"
        const val REWARDED_WIRE = 3
    }
}
