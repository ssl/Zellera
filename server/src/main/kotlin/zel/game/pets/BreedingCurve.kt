package zel.game.pets

import java.util.Random
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

/**
 * A baby's chances per rarity tier, commonest first: a bell around the parents' average level, [widthAtLevelZero]
 * wide and narrowing by [narrowingPerLevel] a level, cut at the level each tier reaches up to ([tierCeilings]).
 */
data class BreedingCurve(
    val widthAtLevelZero: Double = 10.0,
    val narrowingPerLevel: Double = 0.25,
    val tierCeilings: List<Double> = TIER_CEILINGS,
) {
    /**
     * Whole percentages per tier that add up to 100. What lies above the top ceiling, and what rounding leaves
     * over, goes to the rarest tier, so a higher level never gives commoner babies.
     */
    fun tierChances(averageLevel: Double): List<Int> {
        val width = max(MIN_WIDTH, widthAtLevelZero - averageLevel * narrowingPerLevel)
        val reached = tierCeilings.map { (below((it - averageLevel) / width) * PERCENT).toInt() }
        val shares = reached.mapIndexed { tier, upTo -> upTo - (reached.getOrNull(tier - 1) ?: 0) }
        return shares.dropLast(1) + (shares.last() + PERCENT - reached.last())
    }

    /** The share of a standard bell below [deviations] (a polynomial approximation, good to seven places). */
    private fun below(deviations: Double): Double {
        val x = abs(deviations) / sqrt(2.0)
        val t = 1.0 / (1.0 + ERF_P * x)
        val series = ERF_A.foldRight(0.0) { a, sum -> (sum + a) * t }
        val erf = 1.0 - series * exp(-x * x)
        return if (deviations >= 0) (1.0 + erf) / 2 else (1.0 - erf) / 2
    }

    /** The [tierChances] by tier. */
    fun chances(averageLevel: Double): Map<BabyTier, Int> =
        BabyTier.entries
            .reversed()
            .zip(tierChances(averageLevel))
            .toMap()

    /**
     * A baby's breed: its tier is drawn with exactly the [chances] the breeding window shows, the breed evenly from
     * that tier's breeds; a tier without breeds gives way to the next commoner one. Null when none of them has any.
     */
    fun drawBreed(
        averageLevel: Double,
        breeds: Map<BabyTier, List<Int>>,
        random: Random,
    ): Int? {
        var left = random.nextInt(PERCENT)
        val drawn =
            chances(averageLevel).entries.firstOrNull { (_, chance) ->
                left -= chance
                left < 0
            }
        val from = generateSequence(drawn?.key ?: BabyTier.COMMON) { it.commoner }
        val pool = from.firstNotNullOfOrNull { breeds[it]?.takeIf(List<Int>::isNotEmpty) }
        return pool?.get(random.nextInt(pool.size))
    }

    private companion object {
        const val PERCENT = 100
        const val MIN_WIDTH = 0.5
        const val ERF_P = 0.3275911
        val ERF_A = listOf(0.254829592, -0.284496736, 1.421413741, -1.453152027, 1.061405429)
        val TIER_CEILINGS = listOf(10.0, 15.0, 18.0, 20.0)
    }
}
