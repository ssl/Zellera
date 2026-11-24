package zel.wired.effect.user

import zel.protocol.generated.WiredRewardResultOut
import zel.protocol.generated.WiredRewardResultOutCodec
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.util.SECONDS_PER_DAY
import zel.util.SECONDS_PER_HOUR
import zel.util.SECONDS_PER_MINUTE
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import kotlin.random.Random

class GiveReward(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val host: UserEffectHost,
    private val random: Random,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    private val rewards = StaffReward.parse(settings.text)
    private val frequency = settings.ints.getOrNull(FREQUENCY) ?: ONCE
    private val unique = settings.ints.getOrNull(UNIQUE) == 1
    private val limit = settings.ints.getOrNull(LIMIT) ?: 0
    private val interval = settings.ints.getOrNull(INTERVAL) ?: 0
    private val history = HashMap<Int, MutableList<GivenReward>>()

    override fun run(firing: Firing) {
        UserTargets.players(firing, this).forEach { (_, playerId) ->
            firing.scope.out.send(
                Lane.Now,
                Audience.Player(playerId),
                WiredRewardResultOutCodec,
                WiredRewardResultOut(give(playerId)),
            )
        }
    }

    /**
     * The box's own limit is asked first, then what the player had from it: once only, the minute wait, everything
     * collected, the hour and day waits. A player none of them stops draws a reward.
     */
    private fun give(playerId: Int): Int {
        val now = host.unixSeconds()
        val past = history(playerId)
        val refused = if (limit > 0 && host.rewardCount(furniId) >= limit) NO_MORE_AVAILABLE else refusal(past, now)
        if (refused != null) return refused
        val reward = if (unique) rewards.firstOrNull { reward -> past.none { it.code == reward.code } } else roll()
        return hand(playerId, reward, now)
    }

    private fun hand(
        playerId: Int,
        reward: StaffReward?,
        now: Int,
    ): Int =
        when {
            reward == null -> if (unique) ALL_COLLECTED else UNLUCKY
            host.ownsReward(playerId, reward) -> ALREADY_RECEIVED
            !host.giveReward(playerId, reward) -> UNLUCKY
            else -> received(playerId, reward, now)
        }

    private fun history(playerId: Int): MutableList<GivenReward> =
        history.getOrPut(playerId) { host.rewardsGiven(furniId, playerId).toMutableList() }

    private fun received(
        playerId: Int,
        reward: StaffReward,
        now: Int,
    ): Int {
        val entry = GivenReward(reward.code, now)
        history(playerId).add(0, entry)
        host.noteReward(furniId, playerId, entry)
        return if (reward.isBadge) BADGE_RECEIVED else REWARD_RECEIVED
    }

    /** Why a player who had a reward from this box before gets none now; null when nothing stands in the way. */
    private fun refusal(
        past: List<GivenReward>,
        now: Int,
    ): Int? {
        val last = past.firstOrNull() ?: return null
        val since = now - last.at
        return when {
            frequency == ONCE -> ALREADY_RECEIVED
            frequency == MINUTES && since < interval * SECONDS_PER_MINUTE -> RECEIVED_THIS_MINUTE
            unique && rewards.all { reward -> past.any { it.code == reward.code } } -> ALL_COLLECTED
            frequency == HOURS && since < interval * SECONDS_PER_HOUR -> RECEIVED_THIS_HOUR
            frequency == DAYS && since < interval * SECONDS_PER_DAY -> RECEIVED_TODAY
            else -> null
        }
    }

    /** Each reward's chance is its share of a hundred; what the chances leave over is no reward. */
    private fun roll(): StaffReward? {
        var left = random.nextInt(PERCENT)
        return rewards.firstOrNull { reward ->
            left -= reward.chance
            left < 0
        }
    }

    companion object {
        const val CODE = "wf_act_give_reward"
        private const val FREQUENCY = 0
        private const val UNIQUE = 1
        private const val LIMIT = 2
        private const val INTERVAL = 3
        private const val ONCE = 0
        private const val DAYS = 1
        private const val HOURS = 2
        private const val MINUTES = 3
        private const val PERCENT = 100
        private const val NO_MORE_AVAILABLE = 0
        private const val ALREADY_RECEIVED = 1
        private const val RECEIVED_TODAY = 2
        private const val RECEIVED_THIS_HOUR = 3
        private const val UNLUCKY = 4
        private const val ALL_COLLECTED = 5
        private const val REWARD_RECEIVED = 6
        private const val BADGE_RECEIVED = 7
        private const val RECEIVED_THIS_MINUTE = 8
    }
}
