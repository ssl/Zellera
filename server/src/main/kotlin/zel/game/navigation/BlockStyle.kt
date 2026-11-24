package zel.game.navigation

enum class BlockStyle(
    val code: Int,
) {
    ROWS(0),
    TILES(1),
    ;

    companion object {
        fun fromCode(code: Int): BlockStyle = if (code == TILES.code) TILES else ROWS
    }
}
