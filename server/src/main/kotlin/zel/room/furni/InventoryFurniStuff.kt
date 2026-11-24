package zel.room.furni

import zel.game.items.InventoryStuff
import zel.game.items.OwnedFurni
import zel.protocol.generated.StuffData
import zel.room.furni.behaviour.special.SpecialStuff
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Interactions
import zel.room.games.ClockFurni
import zel.wired.chest.ChestFurni
import zel.wired.score.Highscores

class InventoryFurniStuff(
    private val behaviours: () -> FurniBehaviours,
    private val chest: (OwnedFurni) -> StuffData?,
) : InventoryStuff {
    override fun of(item: OwnedFurni): StuffData {
        val behaviour = behaviours().forType(Interactions.behaviourKey(item.definition))
        if (behaviour is ChestFurni) return chest(item) ?: legacy(item, "")
        if (behaviour is ClockFurni) return legacy(item, item.state)
        val furni =
            RoomFurni(
                id = item.id,
                ownerId = item.userId,
                ownerName = "",
                definition = item.definition,
                behaviour = behaviour,
                anchor = null,
                z = 0.0,
                direction = 0,
                state = item.state,
                wallPosition = null,
                groupId = item.guildId,
                limitedData = "${item.editionSize}:${item.editionNumber}",
                rowId = item.dbId,
            )
        return if (behaviour is Highscores) behaviour.inventoryStuff(furni) else FurniWire.stuff(furni)
    }

    private fun legacy(
        item: OwnedFurni,
        state: String,
    ): StuffData {
        val limited = if (item.isLimited) StuffData.Limited(item.editionNumber, item.editionSize) else null
        return SpecialStuff.of(SpecialStuff.LEGACY, StuffData.Value.Legacy(state), limited)
    }
}
