package zel.room.furni.behaviour.use

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.furni.behaviour.state.ArrivalWatch
import zel.room.furni.behaviour.state.Near
import zel.room.furni.behaviour.state.StateShow
import zel.room.unit.path.PathResult
import zel.room.unit.walk.Walking

class PetBowl(
    private val walking: Walking,
    later: (RoomScope, RoomFurni, Long, (RoomScope, RoomFurni) -> Unit) -> Unit,
) : Usable {
    /** The bowl fills as soon as the unit steps on the tile, even while it walks on. */
    private val arrival = ArrivalWatch(later, WATCH_TICKS, mustStop = false) { scope, bowl, _ -> fill(scope, bowl) }

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val unit = scope.room.units.ofPlayer(playerId) ?: return
        if (Near.adjacent(furni, unit.tile)) return fill(scope, furni)
        val goal = Near.nearest(Near.around(scope, furni), unit.tile) ?: return
        if (walking.walkTo(scope, unit, goal) is PathResult.Found) arrival.watch(scope, furni, unit, goal)
    }

    private fun fill(
        scope: RoomScope,
        bowl: RoomFurni,
    ) = StateShow.data(scope, bowl, FULL)

    private companion object {
        /** The bowl's states run from full at 0 to empty at the last; a refill starts them over. */
        const val FULL = "0"
        const val WATCH_TICKS = 60
    }
}
