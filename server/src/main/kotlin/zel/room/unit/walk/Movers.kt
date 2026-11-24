package zel.room.unit.walk

import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.path.HorseRole
import zel.room.unit.path.Mover

object Movers {
    private val SKATE_EFFECTS = setOf(71, 72)

    fun of(
        unit: RoomUnit,
        walk: UnitWalk,
        hooks: WalkHooks,
    ): Mover {
        val riding = walk.rides ?: walk.riddenBy
        return Mover(
            index = unit.index,
            ignoredUnits = setOfNotNull(unit.index, riding),
            skater = hooks.effect(unit) in SKATE_EFFECTS,
            horse = if (riding != null) HorseRole.Ridden else HorseRole.None,
            // One set serves both: a player's transit furni, a pet's open nests; the passage kind tells them apart.
            letThrough = walk.letThrough,
            nests = walk.letThrough,
            playerId = (unit.kind as? UnitKind.Player)?.playerId,
        )
    }
}
