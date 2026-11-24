package zel.wired.variable.addon

data class LevelProgress(
    val level: Int,
    val xp: Long,
    val progress: Long,
    val percentage: Long,
    val required: Long,
    val remaining: Long,
    val maxed: Boolean,
    val maxLevel: Int,
)
