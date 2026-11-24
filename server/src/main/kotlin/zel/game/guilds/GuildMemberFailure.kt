package zel.game.guilds

enum class GuildMemberFailure(
    val code: Int,
) {
    NO_LONGER_MEMBER(0),
    ALREADY_ACCEPTED(2),
}
