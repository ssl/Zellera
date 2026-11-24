package zel.game.items.mysterybox

enum class BoxColour {
    PURPLE,
    BLUE,
    GREEN,
    YELLOW,
    LILAC,
    ORANGE,
    TURQUOISE,
    RED,
    ;

    val code: String = name.lowercase()

    companion object {
        fun named(text: String?): BoxColour? =
            text?.trim()?.lowercase()?.let { t -> entries.firstOrNull { it.code == t } }
    }
}
