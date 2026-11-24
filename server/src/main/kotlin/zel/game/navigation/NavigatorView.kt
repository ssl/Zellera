package zel.game.navigation

import zel.game.permissions.Privilege
import zel.game.permissions.isAnyOwner
import zel.game.players.Player

interface NavigatorView {
    val name: String

    fun blocks(player: Player): List<NavigatorBlock>

    fun searchBlocks(
        player: Player,
        field: RoomSearchField,
        value: String,
        categoryId: Int,
    ): List<NavigatorBlock> = blocks(player)
}

fun seesHiddenRooms(player: Player): Boolean = player.hasPermission(Privilege.ROOM_ENTER_ANY) || player.isAnyOwner
