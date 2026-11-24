package zel.game.claims

import zel.game.items.Items
import zel.game.players.Player
import zel.protocol.claims.ClaimMessages
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.protocol.outgoing.inventory.InventoryUpdateItem
import java.util.function.IntSupplier

/** A badge first, then the answer, then the furni. */
class ProductClaims(
    private val products: ClaimableProducts,
    private val items: Items,
    private val messages: ClaimMessages,
    private val unixTime: IntSupplier,
) {
    fun alreadyTaken(
        playerId: Int,
        claimCode: String,
    ): Boolean = claimCode.isNotEmpty() && products.takenBy(playerId, claimCode)

    fun claim(
        player: Player,
        claimCode: String,
        choice: Int,
    ) {
        val client = player.client ?: return
        val choices = products.choicesOf(claimCode)
        val now = unixTime.asInt
        val picked = choices[choice]
        val result =
            when {
                choices.isEmpty() -> UNKNOWN
                picked == null || picked.endsAt?.let { it <= now } == true -> UNAVAILABLE
                !products.take(player.profile.id, claimCode, choice, now) -> TAKEN
                else -> CLAIMED
            }
        if (result != CLAIMED || picked == null) {
            messages.result(client, claimCode, result)
            return
        }
        if (picked.badge.isNotBlank()) player.inventory.badges.award(picked.badge, asUnseen = true)
        messages.result(client, claimCode, CLAIMED)
        giveFurni(player, picked.furni)
    }

    private fun giveFurni(
        player: Player,
        code: String,
    ) {
        val definition = code.takeIf { it.isNotBlank() }?.let(items::getItem) ?: return
        val item = items.newFurni(player.profile.id, definition, 0, 0, "") ?: return
        player.inventory.furni.put(item)
        player.client?.send(AddRoomItem(item))
        player.client?.send(InventoryUpdateItem(item))
    }

    private companion object {
        const val CLAIMED = 0
        const val TAKEN = 1
        const val UNKNOWN = 2
        const val UNAVAILABLE = 3
    }
}
