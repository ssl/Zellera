@file:Suppress("MagicNumber") // the literals are the defaults of the settings

package zel.game.games.snowball

import zel.core.Configuration

data class SnowballWeeklyPrizes(
    val rankPrizes: List<String>,
    val luckyPrize: String,
    val luckyCount: Int,
    val luckyFrom: Int,
    val luckyTo: Int,
    val groupPoints: List<Int>,
) {
    companion object {
        fun defaults(): SnowballWeeklyPrizes =
            SnowballWeeklyPrizes(
                emptyList(),
                "",
                3,
                4,
                50,
                listOf(50, 25, 15),
            )

        fun from(config: Configuration): SnowballWeeklyPrizes {
            val defaults = defaults()
            val (from, to) = parseRange(config.getValue("snowball.weekly.lucky.ranks", "4-50"), 4, 50)
            return SnowballWeeklyPrizes(
                parseNames(config.getValue("snowball.weekly.rank_prizes", defaults.rankPrizes.joinToString(","))),
                config.getValue("snowball.weekly.lucky.prize", defaults.luckyPrize).trim(),
                maxOf(0, config.getInt("snowball.weekly.lucky.winners", defaults.luckyCount)),
                from,
                to,
                parseAmounts(config.getValue("snowball.weekly.group_prize.points", "50,25,15")),
            )
        }

        internal fun parseNames(value: String): List<String> =
            value.split(",").filter { it.isNotBlank() }.map { it.trim() }

        internal fun parseAmounts(value: String): List<Int> =
            value.split(",").mapNotNull { it.trim().toIntOrNull() }.map { maxOf(0, it) }

        internal fun parseRange(
            value: String,
            from: Int,
            to: Int,
        ): Pair<Int, Int> {
            val parts = value.split("-")
            if (parts.size == 2) {
                val a = parts[0].trim().toIntOrNull()
                val b = parts[1].trim().toIntOrNull()
                if (a != null && b != null && a >= 1 && b >= a) {
                    return a to b
                }
            }
            return from to to
        }
    }
}
