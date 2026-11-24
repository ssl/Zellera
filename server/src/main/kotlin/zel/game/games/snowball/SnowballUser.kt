package zel.game.games.snowball

import zel.game.players.Player

data class SnowballUser(
    val userId: Int,
    val name: String,
    val figure: String,
    val gender: String,
    val motto: String = "",
) {
    companion object {
        fun of(player: Player): SnowballUser {
            val profile = player.profile
            return SnowballUser(profile.id, profile.name, profile.figure, profile.gender.name, profile.motto)
        }
    }
}
