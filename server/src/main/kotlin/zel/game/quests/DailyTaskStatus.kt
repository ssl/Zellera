package zel.game.quests

enum class DailyTaskStatus(
    val code: Int,
) {
    OPEN(0),
    COMPLETED(1),
    CLAIMED(2),
    ;

    companion object {
        /** A stored value no code writes reads as claimed: it can move no further and pays nothing. */
        fun fromCode(code: Int): DailyTaskStatus = entries.firstOrNull { it.code == code } ?: CLAIMED
    }
}
