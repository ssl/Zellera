package zel.protocol.outgoing.users

import zel.game.permissions.PlayerLimits
import zel.game.players.Player
import zel.protocol.generated.FavouritesOut
import zel.protocol.generated.FavouritesOutCodec
import zel.protocol.outgoing.OutgoingMessage

class Favourites(
    val player: Player,
) : OutgoingMessage<FavouritesOut>(FavouritesOutCodec) {
    override fun message(): FavouritesOut {
        val favorites = player.stats.favouriteRooms
        return FavouritesOut(
            PlayerLimits.maxFavouriteRooms(player.profile.rank),
            favorites.all,
        )
    }
}
