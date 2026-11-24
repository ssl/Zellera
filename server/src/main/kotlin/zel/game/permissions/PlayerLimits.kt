package zel.game.permissions

import zel.core.rules.rules
import zel.game.players.Player

/** Room, friend and favourite limits: the game rule raised by the player's rank, never lowered. */
object PlayerLimits {
    fun maxRooms(player: Player): Int =
        maxOf(if (player.stats.club.isMember) rules.maxRoomsClub else rules.maxRooms, player.profile.rank.maxRooms)

    fun maxFriends(player: Player): Int = maxFriends(player.stats.standing.maxFriends, player.profile.rank)

    fun maxFriends(
        ownMax: Int,
        rank: Rank?,
    ): Int = maxOf(ownMax, rank?.maxFriends ?: 0)

    fun maxFriendsShown(
        player: Player,
        club: Boolean,
    ): Int = maxOf(if (club) rules.maxFriendsClub else rules.maxFriends, player.profile.rank.maxFriends)

    fun maxFavouriteRooms(rank: Rank?): Int = maxOf(rules.maxFavouriteRooms, rank?.maxFavouriteRooms ?: 0)
}
