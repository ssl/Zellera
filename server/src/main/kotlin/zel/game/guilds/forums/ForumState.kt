@file:Suppress("MagicNumber") // the numbers the client reads and the table stores

package zel.game.guilds.forums

enum class ForumState(
    val code: Int,
) {
    OPEN(0),
    RESTORED(1),
    HIDDEN_BY_GROUP_ADMIN(10),
    HIDDEN_BY_STAFF(20),
    ;

    val isHidden: Boolean get() = this == HIDDEN_BY_GROUP_ADMIN || this == HIDDEN_BY_STAFF

    companion object {
        fun fromCode(code: Int): ForumState = entries.firstOrNull { it.code == code } ?: OPEN
    }
}
