package zel.room.furni.buildpass

import zel.game.items.ProductKind
import zel.room.map.Point

sealed interface BuildPassSpot {
    val type: ProductKind

    data class Floor(
        val tile: Point,
        val direction: Int,
    ) : BuildPassSpot {
        override val type: ProductKind = ProductKind.FLOOR_FURNI
    }

    data class Wall(
        val position: String,
    ) : BuildPassSpot {
        override val type: ProductKind = ProductKind.WALL_FURNI
    }
}
