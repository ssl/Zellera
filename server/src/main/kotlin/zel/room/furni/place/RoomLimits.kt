package zel.room.furni.place

import zel.core.rules.rules
import zel.game.items.Item
import zel.game.items.ProductKind
import zel.room.Room

class RoomLimits(
    private val maxFurni: () -> Int = { rules.roomMaxFurni },
    private val maxNotes: () -> Int = { rules.roomMaxNotes },
) {
    fun check(
        room: Room,
        definition: Item,
    ): PlaceError? {
        if (room.furni.size >= maxFurni()) return PlaceError.MaxFurniture
        val type = Interactions.of(definition)
        val limit = Interactions.ONE_PER_ROOM[type] ?: return null
        return limit.takeIf { room.furni.all().any { furni -> Interactions.of(furni.definition) == type } }
    }

    fun notesLeft(room: Room): Boolean =
        room.furni.all().count {
            it.definition.type == ProductKind.WALL_FURNI && Interactions.of(it.definition) == Interactions.POST_IT
        } < maxNotes()
}
