package zel.wired.chest

import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind

internal fun RoomUnit.playerId(): Int? = (kind as? UnitKind.Player)?.playerId
