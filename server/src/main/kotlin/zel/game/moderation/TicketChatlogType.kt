@file:Suppress("MagicNumber") // the numbers the client reads and the table stores

package zel.game.moderation

enum class TicketChatlogType(
    val code: Int,
) {
    ROOM_TALK(1),
    PRIVATE_TALK(2),
    FORUM_POST(3),
    FORUM_REPLY(4),
    OWN_PICTURE(5),
    ROOM_PICTURE(6),
}
