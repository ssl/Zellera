package zel.room.furni

import zel.game.items.Item

sealed interface FurniKind {
    data object Plain : FurniKind

    data object StackHelper : FurniKind

    data object WalkTile : FurniKind

    data object MultiHeight : FurniKind

    data object Recolorable : FurniKind

    companion object {
        private val byInteraction: Map<String, FurniKind> =
            mapOf(
                "tile.stack" to StackHelper,
                "tile.walk" to WalkTile,
                "height.multi" to MultiHeight,
                "water.float" to MultiHeight,
                "state.recolor" to Recolorable,
            )

        fun of(definition: Item): FurniKind = byInteraction[definition.interaction.name] ?: Plain
    }
}
