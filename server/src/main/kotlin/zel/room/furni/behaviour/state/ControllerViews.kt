package zel.room.furni.behaviour.state

import zel.protocol.generated.FloorHeightMapOut
import zel.room.RoomScope
import zel.room.entry.EntryViews
import zel.room.furni.RoomFurni
import zel.room.map.Point

class ControllerViews(
    private val part: StatePart,
    private val next: EntryViews = EntryViews.None,
) : EntryViews by next {
    override fun configurationStates(
        scope: RoomScope,
        playerId: Int,
    ): List<Boolean> {
        val room = scope.room
        val chooser = part.of(room).chooserHeld || RoomControllers.chooserDisabled(room)
        return RoomControllers.configurationStates(room, chooser)
    }

    override fun shownFurni(
        scope: RoomScope,
        furni: Collection<RoomFurni>,
    ): Collection<RoomFurni> = InvisibleFurni.shown(part, scope.room, next.shownFurni(scope, furni))

    override fun hiddenTiles(scope: RoomScope): Set<Point> =
        next.hiddenTiles(scope) + AreaHide.hiddenFloor(part, scope.room)

    override fun areaHides(scope: RoomScope): List<FloorHeightMapOut.List1Item> =
        next.areaHides(scope) + AreaHide.entryAreas(part, scope.room)
}
