package zel.room.games

@Suppress("MagicNumber") // the box's action numbers
enum class ClockAction(
    val code: Int,
) {
    Start(0),
    Stop(1),
    Reset(2),
    Pause(3),
    Resume(4),
    ;

    companion object {
        fun fromCode(code: Int): ClockAction? = entries.firstOrNull { it.code == code }
    }
}
