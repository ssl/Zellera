package zel.game.pets

import java.util.Random
import kotlin.math.pow

/** Draws a monster plant's rarity, body and colour; the common parts come up far more often. */
object PlantRolls {
    /** The rarity a rare seed starts from. */
    const val RARE_FROM = 8

    /** Squaring a uniform draw puts most of the weight on the start of the list. */
    private const val SKEW = 2.0
    private const val RARE_LOTS = 66
    private const val RARE_LOT_BASE = 11
    private const val RARE_UP_TO = 10

    /** A fresh seed's rarity is that of a body and a colour each drawn leaning to the common ones. */
    fun freshRarity(random: Random): Int =
        commonLeaning(random).let { (body, colour) -> MonsterPlant.rarityFor(body, colour) }

    fun commonLeaning(random: Random): Pair<Int, Int> =
        leaning(MonsterPlant.BODIES.keys, random) to leaning(MonsterPlant.COLOURS.keys, random)

    /** A rare seed's lots: each rarity takes its share, the highest a rare seed has all that is left. */
    fun freshRareRarity(random: Random): Int {
        val lot = random.nextInt(RARE_LOTS)
        var taken = 0
        for (rarity in RARE_FROM..RARE_UP_TO) {
            taken += RARE_LOT_BASE - rarity
            if (lot <= taken) return rarity
        }
        return RARE_UP_TO
    }

    private fun leaning(
        ids: Collection<Int>,
        random: Random,
    ): Int {
        val draw = random.nextDouble().pow(SKEW)
        return ids.elementAt((ids.size * draw).toInt().coerceAtMost(ids.size - 1))
    }

    /** A body and colour whose rarities add up to [rarity], every such pair equally likely. */
    fun ofRarity(
        rarity: Int,
        random: Random,
    ): Pair<Int, Int> {
        val wanted = rarity.coerceIn(0, MonsterPlant.TOP_RARITY)
        val fitting =
            MonsterPlant.BODIES.entries.flatMap { body ->
                MonsterPlant.COLOURS.entries
                    .filter { colour -> body.value.rarity + colour.value.rarity == wanted }
                    .map { colour -> body.key to colour.key }
            }
        return fitting[random.nextInt(fitting.size)]
    }
}
