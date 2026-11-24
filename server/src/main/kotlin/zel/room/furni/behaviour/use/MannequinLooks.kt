package zel.room.furni.behaviour.use

import zel.game.players.Gender
import zel.game.players.Player
import zel.protocol.outgoing.users.UserData
import zel.room.RoomScope
import zel.room.port.RoomPort

class MannequinLooks(
    private val player: (Int) -> Player?,
    private val rooms: () -> RoomPort?,
) : Mannequin.LookChanges {
    override fun genderOf(playerId: Int): Gender? = player(playerId)?.profile?.gender

    override fun lookOf(playerId: Int): String = player(playerId)?.profile?.figure.orEmpty()

    override fun wear(
        scope: RoomScope,
        playerId: Int,
        look: String,
    ) {
        val wearer = player(playerId) ?: return
        wearer.profile.figure = look
        rooms()?.appearanceChanged(playerId)
        wearer.client?.send(UserData(wearer))
    }
}
