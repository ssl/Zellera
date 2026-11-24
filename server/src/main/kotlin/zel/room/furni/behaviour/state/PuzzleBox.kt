package zel.room.furni.behaviour.state

import zel.protocol.generated.SlideObjectBundleOutCodec
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.RoomHeights
import zel.room.furni.Usable
import zel.room.furni.roller.RollerSlides
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import kotlin.math.abs

class PuzzleBox(
    private val part: StatePart,
) : Usable {
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val room = scope.room
        val unit = room.units.ofPlayer(playerId) ?: return
        val box = furni.anchor ?: return
        val dx = box.x - unit.tile.x
        val dy = box.y - unit.tile.y
        if (abs(dx) + abs(dy) != 1) {
            Near.walkBeside(scope, part.hooks, furni, unit)
            return
        }
        StateShow.whole(scope, furni, Multistate.stepped(furni, backward = false) ?: furni.state)
        val to = Point(box.x + dx, box.y + dy)
        if (!takes(room, to)) return
        val fromZ = furni.z
        val toZ = RoomHeights.stackingHeight(room, listOf(to), furni) ?: return
        room.furni.move(furni, to, furni.direction)
        furni.z = toZ
        furni.dirty = true
        part.hooks.heightsChanged(scope, listOf(box, to))
        val slide = RollerSlides.furni(box, to, furni.id, fromZ, toZ, NO_ROLLER)
        scope.out.send(Lane.AfterUnitUpdate, Audience.Room, SlideObjectBundleOutCodec, slide)
        part.hooks.walkTo(scope, unit, box)
    }

    private fun takes(
        room: Room,
        tile: Point,
    ): Boolean {
        val top = room.furni.at(tile).topItem()
        return room.floor.isFloor(tile) &&
            room.units.all().none { it.tile == tile } &&
            (top == null || top.definition.canStack)
    }

    private companion object {
        const val NO_ROLLER = -1
    }
}
