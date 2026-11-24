package zel.protocol.outgoing.inventory

import zel.game.items.OwnedFurni
import zel.protocol.generated.FurniListAddOrUpdateOut
import zel.protocol.generated.FurniListAddOrUpdateOutCodec
import zel.protocol.outgoing.OutgoingMessage

class InventoryUpdateItem(
    private val items: Collection<OwnedFurni>,
) : OutgoingMessage<FurniListAddOrUpdateOut>(FurniListAddOrUpdateOutCodec) {
    constructor(item: OwnedFurni) : this(listOf(item))

    override fun message() = FurniListAddOrUpdateOut(items.map(::item))

    private fun item(furni: OwnedFurni): FurniListAddOrUpdateOut.ItemsItem {
        val entry = InventoryFurni.entry(furni)
        return FurniListAddOrUpdateOut.ItemsItem(
            stripId = entry.stripId,
            type = entry.type,
            itemId = entry.itemId,
            spriteId = entry.spriteId,
            category = entry.category,
            stuffData = entry.stuffData,
            recyclable = entry.recyclable,
            tradable = entry.tradable,
            groupable = entry.groupable,
            sellable = entry.sellable,
            secondsToExpiration = InventoryFurni.NEVER_EXPIRES,
            rentPeriodStarted = false,
            roomId = InventoryFurni.NOT_IN_A_ROOM,
            floorExtras =
                entry.floorExtra?.let { FurniListAddOrUpdateOut.ItemsItem.FloorExtras.Then("", it) }
                    ?: FurniListAddOrUpdateOut.ItemsItem.FloorExtras.Else,
        )
    }
}
