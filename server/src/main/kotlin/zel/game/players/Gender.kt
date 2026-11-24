package zel.game.players

enum class Gender {
    M,
    F,
    ;

    companion object {
        fun ofCode(code: String): Gender? = entries.firstOrNull { it.name.equals(code, ignoreCase = true) }
    }
}
