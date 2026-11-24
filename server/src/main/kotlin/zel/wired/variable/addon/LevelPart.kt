package zel.wired.variable.addon

@Suppress("MagicNumber") // the bits of the form's mask
enum class LevelPart(
    val bit: Int,
    val partName: String,
) {
    CURRENT_LEVEL(0, "current_level"),
    CURRENT_XP(1, "current_xp"),
    PROGRESS(2, "progress"),
    PERCENTAGE(3, "progress_percentage"),
    REQUIRED(4, "xp_required"),
    REMAINING(5, "xp_remaining"),
    IS_MAXED(6, "is_maxed"),
    MAX_LEVEL(7, "max_level"),
    ;

    fun of(progress: LevelProgress): Long =
        when (this) {
            CURRENT_LEVEL -> progress.level.toLong()
            CURRENT_XP -> progress.xp
            PROGRESS -> progress.progress
            PERCENTAGE -> progress.percentage
            REQUIRED -> progress.required
            REMAINING -> progress.remaining
            IS_MAXED -> 0
            MAX_LEVEL -> progress.maxLevel.toLong()
        }
}
