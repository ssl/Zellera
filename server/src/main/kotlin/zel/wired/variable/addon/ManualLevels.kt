package zel.wired.variable.addon

import java.util.TreeMap

class ManualLevels(
    lines: List<Pair<Int, Int>>,
) : LevelTable {
    private val levelAt = TreeMap<Long, Int>().also { map -> lines.forEach { (l, xp) -> map[xp.toLong()] = l } }
    override val maxXp: Long = if (levelAt.isEmpty()) 0 else levelAt.lastKey()
    override val maxLevel: Int = if (levelAt.isEmpty()) 1 else levelAt.lastEntry().value

    override fun xpForLevel(level: Int): Long {
        if (level <= 1) return 0
        var last = 0L
        for ((xp, atLevel) in levelAt) {
            if (atLevel == level) return xp
            if (atLevel > level) {
                val lower = levelAt.lowerEntry(xp)
                val fromLevel = lower?.value ?: 1
                val fromXp = lower?.key ?: 0L
                return fromXp + ((xp - fromXp).toDouble() / (atLevel - fromLevel) * (level - fromLevel)).toLong()
            }
            last = xp
        }
        return last
    }

    override fun read(value: Long): LevelProgress {
        val xp = value.coerceIn(0, maxXp)
        val ceiling = levelAt.ceilingEntry(xp + 1)
        if (ceiling == null) return LevelProgress(maxLevel, xp, 0, 0, 0, 0, true, maxLevel)
        val floor = levelAt.floorEntry(xp)
        val fromLevel = floor?.value ?: 1
        val fromXp = floor?.key ?: 0L
        val levels = ceiling.value - fromLevel
        val perLevel = (ceiling.key - fromXp).toDouble() / levels
        val done = xp - fromXp
        var steps = maxOf((done / perLevel).toInt(), 0).coerceAtMost(levels - 1)
        var start = fromXp + (perLevel * steps).toLong()
        var next = nextStart(steps, levels, ceiling.key, fromXp, perLevel)
        if (steps != levels - 1 && xp >= next) {
            steps++
            start = fromXp + (perLevel * steps).toLong()
            next = nextStart(steps, levels, ceiling.key, fromXp, perLevel)
        }
        val required = next - start
        val percentage = ((xp - start).toDouble() / required * LevelTable.PERCENT).toLong()
        return LevelProgress(fromLevel + steps, xp, xp - start, percentage, required, next - xp, false, maxLevel)
    }

    private fun nextStart(
        steps: Int,
        levels: Int,
        ceilingXp: Long,
        fromXp: Long,
        perLevel: Double,
    ): Long = if (steps == levels - 1) ceilingXp else fromXp + (perLevel * (steps + 1)).toLong()
}
