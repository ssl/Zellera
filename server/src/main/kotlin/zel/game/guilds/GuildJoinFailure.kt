package zel.game.guilds

enum class GuildJoinFailure(
    val code: Int,
) {
    FULL(0),
    TOO_MANY_GUILDS(1),
    CLOSED(2),
}
