package zel.room.side

import zel.game.catalog.Catalog
import zel.game.navigation.Navigator

data class RoomSideServices(
    val navigator: () -> Navigator?,
    val catalog: () -> Catalog?,
    val staffPicksCategory: () -> Int,
    val progress: (playerId: Int, event: String, amount: Int) -> Unit,
)
