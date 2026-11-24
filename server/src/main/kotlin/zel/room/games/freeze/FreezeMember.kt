package zel.room.games.freeze

import zel.room.games.FreezeRules
import zel.room.games.TeamColor
import zel.room.games.TeamMember
import zel.room.unit.RoomUnit
import kotlin.math.max

/** What one player holds during a round: lives, balls in hand, how far and in which shape the next ball bursts. */
class FreezeMember(
    unit: RoomUnit,
    playerId: Int,
    color: TeamColor,
) : TeamMember(unit, playerId, color) {
    /** The effects and lives the field's furni name; set when the player joins and again when a round starts. */
    var kit = FreezeKit.standard(color)

    var lives = kit.lives
        private set

    var out = false

    private var ballsInHand = START_BALLS
    private var extraReach = 0
    private var nextBurst = Burst.Cross
    private var protectedSeconds = 0
    private var frozenSeconds = 0

    var frozeOpponents = 0

    var effectDelay = EFFECT_DELAY_PER_TILE
        private set

    val isFrozen: Boolean get() = frozenSeconds > 0
    val isProtected: Boolean get() = protectedSeconds > 0
    val canThrow: Boolean get() = ballsInHand > 0 && !isFrozen && !out
    val canBeFrozen: Boolean get() = !isFrozen && !isProtected && !out

    /** The directions a ball bursts in. */
    enum class Burst(
        val straight: Boolean,
        val diagonal: Boolean,
    ) {
        Cross(straight = true, diagonal = false),
        Diagonal(straight = false, diagonal = true),
        Star(straight = true, diagonal = true),
    }

    override fun reset() {
        lives = kit.lives
        ballsInHand = START_BALLS
        extraReach = 0
        nextBurst = Burst.Cross
        protectedSeconds = 0
        frozenSeconds = 0
        frozeOpponents = 0
        out = false
        super.reset()
    }

    /** Takes a ball out of the hand; a special burst is used up by the one ball it was picked up for. */
    fun throwBall(): BallShape {
        ballsInHand--
        val burst = nextBurst
        nextBurst = Burst.Cross
        // A star ball bursts at the longest reach whatever its thrower has collected.
        val reach = if (burst == Burst.Star) MAX_EXTRA_REACH else extraReach
        return BallShape(FreezeGame.BALL_REACH + reach, burst.straight, burst.diagonal)
    }

    /** A burst ball or a picked-up one comes to the hand while there is room for it. */
    fun addBall(rules: FreezeRules) {
        if (ballsInHand < max(START_BALLS, rules.maxSnowballs)) ballsInHand++
    }

    fun mayTakeLife(rules: FreezeRules): Boolean = lives < rules.maxLives

    fun addLife(rules: FreezeRules): Boolean = mayTakeLife(rules).also { if (it) lives++ }

    fun reachFurther() {
        if (extraReach < MAX_EXTRA_REACH) extraReach++
    }

    fun burstNext(burst: Burst) {
        nextBurst = burst
    }

    fun addProtection(rules: FreezeRules) {
        if (!isProtected || rules.protectionStacks) protectedSeconds += rules.protectSeconds
    }

    fun freeze(
        distance: Int,
        rules: FreezeRules,
    ) {
        lives = max(0, lives - 1)
        effectDelay = max(1, distance) * EFFECT_DELAY_PER_TILE
        frozenSeconds = rules.frozenSeconds
        ballsInHand = max(START_BALLS, ballsInHand - rules.frozenLoseSnowballs)
        extraReach = (extraReach - rules.frozenLoseBoost).coerceIn(0, MAX_EXTRA_REACH)
    }

    fun thaw() {
        frozenSeconds = 0
    }

    fun cycle(): Tick {
        val wasProtected = isProtected
        if (wasProtected) protectedSeconds--
        val wasFrozen = isFrozen
        if (wasFrozen) frozenSeconds--
        return when {
            wasFrozen && !isFrozen && lives <= 0 -> {
                Tick.Out
            }

            wasFrozen && !isFrozen -> {
                protectedSeconds = max(protectedSeconds, PROTECTED_AFTER_FROZEN)
                Tick.Thawed
            }

            wasProtected && !isProtected -> {
                Tick.EffectChanged
            }

            else -> {
                Tick.Nothing
            }
        }
    }

    enum class Tick {
        Nothing,
        EffectChanged,
        Thawed,
        Out,
    }

    fun effect(): Int =
        when {
            out -> 0
            isFrozen -> kit.frozenEffect
            isProtected -> kit.shieldedEffect
            else -> kit.teamEffect
        }

    companion object {
        private const val PROTECTED_AFTER_FROZEN = 5
        private const val EFFECT_DELAY_PER_TILE = 100
        private const val START_BALLS = 1
        private const val MAX_EXTRA_REACH = 5
    }
}
