package zel.wired.effect.furni

import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.wired.firing.Firing

class MoveTrial(
    private val moves: FurniMoves,
    private val firing: Firing,
) {
    private class Moved(
        val furni: RoomFurni,
        val anchor: Point,
        val direction: Int,
        val z: Double,
    )

    private val made = ArrayList<Moved>()

    var refused = false
        private set

    fun move(
        furni: RoomFurni,
        tile: Point,
        direction: Int = furni.direction,
        unitsBlock: Boolean = true,
    ) {
        if (refused) return
        val anchor = furni.anchor ?: return
        val lands = moves.allowedLanding(firing, furni, tile, direction, unitsBlock)
        if (lands == null) {
            refused = true
            return
        }
        made += Moved(furni, anchor, furni.direction, furni.z)
        firing.scope.room.furni
            .move(furni, tile, direction)
        furni.z = lands.z
    }

    fun lands(
        furni: RoomFurni,
        tile: Point,
        unitsBlock: Boolean = true,
    ): Boolean = moves.allowedLanding(firing, furni, tile, unitsBlock = unitsBlock) != null

    fun refuse() {
        refused = true
    }

    fun undo() {
        made.asReversed().forEach {
            firing.scope.room.furni
                .move(it.furni, it.anchor, it.direction)
            it.furni.z = it.z
        }
        made.clear()
    }
}
