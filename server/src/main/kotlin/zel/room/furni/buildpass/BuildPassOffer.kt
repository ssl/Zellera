package zel.room.furni.buildpass

import zel.game.items.Item

data class BuildPassOffer(
    val definition: Item,
    val state: String,
    val clubOnly: Boolean,
)
