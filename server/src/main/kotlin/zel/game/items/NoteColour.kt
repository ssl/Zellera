package zel.game.items

enum class NoteColour(
    val rgb: String,
) {
    SKY("9CCEFF"),
    MINT("9CFF9C"),
    AQUA("9CFFFF"),
    ROSE("FF9C9C"),
    LILAC("FF9CFF"),
    PEACH("FFCC66"),
    LEMON("FFFF33"),
    PAPER("FFFFFF"),
    ;

    companion object {
        private val BY_RGB = entries.associateBy { it.rgb }

        fun named(rgb: String?): NoteColour? = BY_RGB[rgb?.trim()?.uppercase()]

        fun onPole(asked: String?): String = (named(asked) ?: LEMON).rgb
    }
}
