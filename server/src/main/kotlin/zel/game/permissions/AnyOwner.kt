package zel.game.permissions

import zel.game.players.Player

val Player.isAnyOwner: Boolean get() = hasPermission(Privilege.ROOM_MANAGE_ANY)

val Player.isAnyGroupOwner: Boolean get() = hasPermission(Privilege.GROUP_MANAGE_ANY)
