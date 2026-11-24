package zel.game.guilds

enum class GuildEditFailure(
    val code: Int,
) {
    ROOM_TAKEN(0),
    BAD_NAME(1),
    CLUB_REQUIRED(2),
}
