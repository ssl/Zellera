package zel.game.pets

import zel.game.players.Player

object PetRespectPoints {
    fun take(player: Player): Boolean =
        synchronized(player.stats) {
            if (player.stats.tally.petRespectsLeft <= 0) {
                false
            } else {
                player.stats.tally.petRespectsLeft--
                true
            }
        }
}
