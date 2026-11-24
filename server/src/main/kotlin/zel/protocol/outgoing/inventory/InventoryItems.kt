package zel.protocol.outgoing.inventory

import zel.game.items.OwnedFurni
import zel.protocol.generated.FurniListOut
import zel.protocol.generated.FurniListOutCodec
import zel.protocol.outgoing.OutgoingMessage
import org.slf4j.LoggerFactory

/** Fragments are numbered from 0; an empty inventory is one empty fragment. */
class InventoryItems(
    private val fragment: Int,
    private val fragments: Int,
    private val items: List<OwnedFurni>,
) : OutgoingMessage<FurniListOut>(FurniListOutCodec) {
    /** An unwritable furni is left out, but the fragment still goes: the client waits for all of them. */
    override fun message(): FurniListOut =
        FurniListOut(totalFragments = fragments, fragmentNo = fragment, items = items.mapNotNull(::itemOrNull))

    private fun itemOrNull(furni: OwnedFurni): FurniListOut.ItemsItem? =
        try {
            item(furni)
        } catch (e: IllegalArgumentException) {
            log.error("Inventory furni {} left out of fragment {} of {}", furni.id, fragment, fragments, e)
            null
        }

    private fun item(furni: OwnedFurni): FurniListOut.ItemsItem {
        val entry = InventoryFurni.entry(furni)
        return FurniListOut.ItemsItem(
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
                entry.floorExtra?.let { FurniListOut.ItemsItem.FloorExtras.Then("", it) }
                    ?: FurniListOut.ItemsItem.FloorExtras.Else,
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(InventoryItems::class.java)

        fun fragments(
            owned: List<OwnedFurni>,
            size: Int,
        ): List<InventoryItems> {
            if (owned.isEmpty()) return listOf(InventoryItems(0, 1, emptyList()))
            val parts = owned.chunked(size)
            return parts.mapIndexed { index, items -> InventoryItems(index, parts.size, items) }
        }
    }
}
