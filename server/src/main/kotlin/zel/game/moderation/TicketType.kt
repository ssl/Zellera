@file:Suppress("MagicNumber") // the numbers the client reads and the table stores

package zel.game.moderation

enum class TicketType(
    val code: Int,
) {
    NORMAL(1),
    AUTOMATIC(3),
    AUTO_MESSENGER(4),
    GUIDE(5),
    MESSENGER(6),
    ROOM(7),
    PANIC(8),
    GUARDIAN(9),
    AUTO_HELPER(10),
    FORUM(11),
    SELFIE(12),
    PHOTO(14),
    AMBASSADOR(15),
    ;

    companion object {
        fun fromCode(code: Int): TicketType = entries.firstOrNull { it.code == code } ?: NORMAL
    }
}
