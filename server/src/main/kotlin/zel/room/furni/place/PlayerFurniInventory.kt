package zel.room.furni.place

import zel.game.items.OwnedFurni
import zel.game.players.Player
import zel.game.players.inventory.FurniStash
import zel.protocol.generated.FurniListAddOrUpdateOut
import zel.protocol.generated.FurniListAddOrUpdateOutCodec
import zel.protocol.generated.FurniListInvalidateOut
import zel.protocol.generated.FurniListInvalidateOutCodec
import zel.protocol.outgoing.inventory.InventoryFurni

/** A guild gate goes back into a room closed, whatever state it was picked up in. */
private const val GUILD_GATE = "gate.group"

class PlayerFurniInventory(
    private val players: (Int) -> Player?,
    private val load: (Int) -> OwnedFurni?,
) : FurniInventory {
    override fun find(
        playerId: Int,
        itemId: Int,
    ): InventoryItem? {
        val player = players(playerId) ?: return null
        val item = items(player)?.get(itemId) ?: return null
        return InventoryItem(
            id = item.id,
            ownerId = player.profile.id,
            ownerName = player.profile.name,
            definition = item.definition,
            state = if (item.definition.interaction.name == GUILD_GATE) "" else item.storedState,
            limitedData = "${item.editionSize}:${item.editionNumber}",
            groupId = item.guildId,
        )
    }

    override fun take(
        playerId: Int,
        itemId: Int,
    ): Boolean {
        val items = players(playerId)?.let(::items) ?: return false
        return items.takeOut(itemId) != null
    }

    override fun handBack(
        ownerId: Int,
        furniId: Int,
    ) = handBackAll(ownerId, listOf(furniId))

    override fun handBackAll(
        ownerId: Int,
        furniIds: List<Int>,
    ) {
        val owner = players(ownerId) ?: return
        val inventory = items(owner) ?: return
        val back = furniIds.mapNotNull(load)
        back.forEach(inventory::put)
        val client = owner.client ?: return
        if (back.isEmpty()) return
        client.send(FurniListAddOrUpdateOutCodec, FurniListAddOrUpdateOut(back.map(::entry)))
    }

    override fun handBackQuietly(
        ownerId: Int,
        furniIds: List<Int>,
    ) {
        val owner = players(ownerId) ?: return
        val inventory = items(owner) ?: return
        furniIds.mapNotNull(load).forEach(inventory::put)
    }

    override fun refresh(ownerId: Int) {
        players(ownerId)?.client?.send(FurniListInvalidateOutCodec, FurniListInvalidateOut)
    }

    fun entry(item: OwnedFurni): FurniListAddOrUpdateOut.ItemsItem {
        val e = InventoryFurni.entry(item)
        return FurniListAddOrUpdateOut.ItemsItem(
            stripId = e.stripId,
            type = e.type,
            itemId = e.itemId,
            spriteId = e.spriteId,
            category = e.category,
            stuffData = e.stuffData,
            recyclable = e.recyclable,
            tradable = e.tradable,
            groupable = e.groupable,
            sellable = e.sellable,
            secondsToExpiration = InventoryFurni.NEVER_EXPIRES,
            rentPeriodStarted = false,
            roomId = -1,
            floorExtras =
                e.floorExtra?.let { FurniListAddOrUpdateOut.ItemsItem.FloorExtras.Then("", it) }
                    ?: FurniListAddOrUpdateOut.ItemsItem.FloorExtras.Else,
        )
    }

    private fun items(player: Player): FurniStash? = runCatching { player.inventory.furni }.getOrNull()
}
