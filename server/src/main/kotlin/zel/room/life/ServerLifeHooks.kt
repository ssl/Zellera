package zel.room.life

import zel.game.achievements.GameEvents
import zel.game.players.Player
import zel.game.quests.PlayerActions
import zel.room.listing.RoomCategory

class ServerLifeHooks(
    /** The room categories as last read, never read from the database here: this runs on the room lane. */
    private val categories: () -> List<RoomCategory>?,
    private val progress: (playerId: Int, event: String, amount: Int) -> Unit,
    private val players: (Int) -> Player? = { null },
) : LifeHooks {
    override fun publicCategory(categoryId: Int): Boolean {
        val known = categories() ?: return true // none read yet: keep the room rather than guess
        return known.any { it.id == categoryId && it.isPublic }
    }

    override fun handItemReceived(
        playerId: Int,
        item: Int,
    ) = PlayerActions.tookHandItem(players(playerId), item)

    override fun decoHosting(
        ownerId: Int,
        amount: Int,
    ) = progress(ownerId, GameEvents.ROOM_HOSTING, amount)
}
