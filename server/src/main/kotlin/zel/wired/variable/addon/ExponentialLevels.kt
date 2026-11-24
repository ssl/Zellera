package zel.wired.variable.addon

import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow

class ExponentialLevels(
    private val first: Int,
    percent: Int,
    override val maxLevel: Int,
) : LevelTable {
    private val factor = percent / LevelTable.PERCENT.toDouble()
    override val maxXp: Long = start(maxLevel)

    override fun xpForLevel(level: Int): Long =
        when {
            level < 1 -> 0
            level > maxLevel -> maxXp
            else -> start(level)
        }

    private fun start(level: Int): Long = (first * (((1 + factor).pow(level - 1) - 1 + ROUNDING) / factor)).toLong()

    private fun levelOf(xp: Long): Int {
        if (xp <= 0) return 1
        val guess = (ln(xp * factor / first + 1) / ln(1 + factor)).toInt() + 1
        return when {
            guess > maxLevel -> maxLevel
            guess < 1 -> 1
            xp < xpForLevel(guess) -> maxOf(1, guess - 1)
            xp >= xpForLevel(guess + 1) -> min(maxLevel, guess + 1)
            else -> guess
        }
    }

    override fun read(value: Long): LevelProgress {
        val xp = value.coerceIn(0, maxXp)
        val level = levelOf(xp)
        if (level >= maxLevel) return LevelProgress(level, xp, 0, 0, 0, 0, true, maxLevel)
        val start = xpForLevel(level)
        val next = xpForLevel(level + 1)
        val percentage = if (start == next) LevelTable.PERCENT else (xp - start) * LevelTable.PERCENT / (next - start)
        return LevelProgress(level, xp, xp - start, percentage, next - start, next - xp, false, maxLevel)
    }

    private companion object {
        const val ROUNDING = 1e-9
    }
}
