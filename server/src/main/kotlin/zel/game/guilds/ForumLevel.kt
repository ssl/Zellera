@file:Suppress("MagicNumber") // forum level wire codes

package zel.game.guilds

enum class ForumLevel(
    val code: Int,
) {
    EVERYONE(0),
    MEMBERS(1),
    ADMINS(2),
    OWNER(3),
    ;

    companion object {
        fun fromCode(code: Int): ForumLevel? = entries.firstOrNull { it.code == code }
    }
}
