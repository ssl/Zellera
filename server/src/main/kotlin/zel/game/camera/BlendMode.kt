package zel.game.camera

enum class BlendMode {
    NORMAL,
    ADD,
    MULTIPLY,
    SCREEN,
    OVERLAY,
    HARDLIGHT,
    DARKEN,
    LIGHTEN,
    DIFFERENCE,
    SUBTRACT,
    ;

    companion object {
        private val BY_CLIENT_NAME = entries.associateBy { it.name.lowercase() }

        fun ofSprite(name: String?): BlendMode = BY_CLIENT_NAME[name] ?: NORMAL
    }
}
