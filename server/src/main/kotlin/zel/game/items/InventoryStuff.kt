package zel.game.items

import zel.protocol.generated.StuffData

fun interface InventoryStuff {
    fun of(item: OwnedFurni): StuffData
}
