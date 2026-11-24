package zel.room.furni.behaviour.state

import zel.game.achievements.ExploreActivityAchievements
import zel.game.achievements.GameEvents
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.path.ClosedToWalks
import zel.util.MILLIS_PER_SECOND

class GymEquipment(
    private val part: StatePart,
    private val tile: EffectTile = EffectTile(part),
) : Steppable by tile,
    ClosedToWalks {
    override fun closed(
        room: Room,
        furni: RoomFurni,
    ): Boolean {
        val session = part.of(room).gym[furni.id] ?: return false
        return room.units[session.unitIndex]?.tile?.let { it in furni.tiles() } == true
    }

    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        tile.onStepOn(scope, furni, unit)
        if (unit.kind is UnitKind.Pet) return
        unit.face(furni.direction)
        part.of(scope.room).gym[furni.id] = GymSession(unit.index, scope.now())
    }

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        tile.onStepOff(scope, furni, unit)
        if (unit.kind is UnitKind.Pet) return
        part.hooks.giveEffect(scope, unit, 0, null)
        val session =
            part
                .of(scope.room)
                .gym
                .remove(furni.id)
                ?.takeIf { it.unitIndex == unit.index }
        val player = unit.kind as? UnitKind.Player
        if (session != null && player != null) {
            val minutes = ((scope.now() - session.startedAt) / MILLIS_PER_POINT).toInt()
            val event = GameEvents.of(GameEvents.FURNI_EXERCISED, furni.definition.name)
            if (minutes > 0) part.hooks.progress(player.playerId, event, minutes)
        }
        StateShow.whole(scope, furni, IDLE)
    }

    private companion object {
        const val IDLE = "0"
        const val MILLIS_PER_POINT = ExploreActivityAchievements.GYM_SECONDS_PER_POINT * MILLIS_PER_SECOND
    }
}
