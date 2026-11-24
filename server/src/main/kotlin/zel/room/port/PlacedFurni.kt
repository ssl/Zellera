package zel.room.port

import zel.game.items.Item

data class PlacedFurni(
    val id: Int,
    val ownerId: Int,
    val definition: Item,
)
