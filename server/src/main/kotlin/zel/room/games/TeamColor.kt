package zel.room.games

@Suppress("MagicNumber") // the protocol's colour numbers
enum class TeamColor(
    val code: Int,
) {
    RED(1),
    GREEN(2),
    BLUE(3),
    YELLOW(4),
    ;

    companion object {
        fun fromCode(code: Int): TeamColor? = entries.firstOrNull { it.code == code }
    }
}
