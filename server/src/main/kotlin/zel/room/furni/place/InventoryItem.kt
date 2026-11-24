package zel.room.furni.place

import zel.game.items.Item

data class InventoryItem(
    val id: Int,
    val ownerId: Int,
    val ownerName: String,
    val definition: Item,
    val state: String,
    val limitedData: String,
    val groupId: Int,
)
