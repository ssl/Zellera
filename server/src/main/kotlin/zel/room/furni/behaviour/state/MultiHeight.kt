package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.HeightMapChange
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniOut

class MultiHeight(
    part: StatePart,
) : Multistate(part) {
    override fun clicked(
        scope: RoomScope,
        furni: RoomFurni,
    ): String? {
        val anchor = furni.anchor ?: return null
        val top =
            scope.room.furni
                .at(anchor)
                .topItem()
        if (top != null && top !== furni) return null
        return next(furni)
    }

    override fun show(
        scope: RoomScope,
        furni: RoomFurni,
        state: String,
    ) {
        val change = HeightMapChange.of(scope.room, furni.tiles())
        StateShow.whole(scope, furni, state)
        FurniOut.heights(scope, change)
        part.hooks.heightsChanged(scope, furni.tiles())
    }

    override fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean {
        val heights = furni.definition.stateHeights.size
        if (heights <= 1) return super.toggle(scope, furni, backward)
        val current = furni.state.trim().toIntOrNull() ?: 0
        StateShow.set(furni, Math.floorMod(current + if (backward) -1 else 1, heights).toString())
        part.hooks.heightsChanged(scope, furni.tiles())
        return true
    }

    private fun next(furni: RoomFurni): String? {
        val heights = furni.definition.stateHeights.size
        if (heights == 0) return null
        return ((furni.state.trim().toIntOrNull() ?: 0) + 1).mod(heights).toString()
    }
}
