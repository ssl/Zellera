package zel.wired.variable.addon

import kotlin.math.min

class LinearLevels(
    private val step: Int,
    override val maxLevel: Int,
) : LevelTable {
    override val maxXp: Long = step.toLong() * (maxLevel - 1)

    override fun xpForLevel(level: Int): Long = step.toLong() * (level.coerceIn(1, maxLevel) - 1)

    override fun read(value: Long): LevelProgress {
        val xp = value.coerceIn(0, maxXp)
        val level = min(xp / step + 1, maxLevel.toLong()).toInt()
        if (level >= maxLevel) return LevelProgress(level, xp, 0, 0, 0, 0, true, maxLevel)
        val progress = xp % step
        val percentage = progress * LevelTable.PERCENT / step
        return LevelProgress(level, xp, progress, percentage, step.toLong(), step - progress, false, maxLevel)
    }
}
