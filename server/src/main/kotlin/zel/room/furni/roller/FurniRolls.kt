package zel.room.furni.roller

import zel.game.items.FurniHeights
import zel.protocol.generated.HeightMapUpdateOutCodec
import zel.protocol.generated.SlideObjectBundleOut
import zel.room.Room
import zel.room.furni.RoomFurni
import zel.room.furni.RoomHeights
import zel.room.furni.TileStack
import zel.room.map.MapMessages
import zel.room.map.Point
import zel.room.map.TileValue
import zel.room.tick.Audience
import zel.room.tick.Lane

object FurniRolls {
    private const val EPSILON = 1e-4

    /** Moves the furni the roller carries this pass, top first; their bundle items are for the caller to send. */
    fun roll(
        pass: RollerPass,
        roller: RoomFurni,
        target: RollerTarget,
    ): List<SlideObjectBundleOut.List1Item> {
        if (!target.takesFurni) return emptyList()
        val room = pass.scope.room
        val from = roller.anchor ?: return emptyList()
        val rollerTop = roller.top
        val carried =
            room.furni
                .at(from)
                .furni
                .filter { it !== roller && it.anchor == from && it.id !in pass.movedFurni }
                .filter { it.z >= rollerTop - EPSILON }
                .sortedWith(compareByDescending<RoomFurni> { it.z }.thenByDescending { it.id })
        if (carried.isEmpty()) return emptyList()
        val base = landing(room, target, carried) ?: return emptyList()
        if (base > rollerTop + EPSILON || !carried.all { fits(room, it, from, target.tile) }) return emptyList()
        val items = carried.map { move(pass, it, target.tile, FurniHeights.stack(base, aboveOf(it, rollerTop))) }
        pass.walking.furniChanged(pass.scope, listOf(from))
        return items
    }

    private fun landing(
        room: Room,
        target: RollerTarget,
        carried: List<RoomFurni>,
    ): Double? {
        val floor = room.floor.floorZ(target.tile) ?: return null
        val others =
            room.furni
                .at(target.tile)
                .furni
                .filter { it !in carried }
        val stack = TileStack(others)
        return stack.stackingHeight(floor) ?: stack.topItem()?.top
    }

    private fun aboveOf(
        furni: RoomFurni,
        rollerTop: Double,
    ): Double = (furni.z.toFloat() - rollerTop.toFloat()).toDouble()

    private fun fits(
        room: Room,
        furni: RoomFurni,
        from: Point,
        to: Point,
    ): Boolean = furni.tiles().all { room.floor.baseHeight(shift(it, from, to)) != null }

    private fun move(
        pass: RollerPass,
        furni: RoomFurni,
        to: Point,
        z: Double,
    ): SlideObjectBundleOut.List1Item {
        val scope = pass.scope
        val room = scope.room
        val fromZ = furni.z
        val oldTiles = furni.tiles()
        room.furni.move(furni, to, furni.direction)
        furni.z = z
        furni.dirty = true
        pass.movedFurni += furni.id
        val tiles = (oldTiles + furni.tiles()).distinct().sortedWith(compareBy<Point> { it.y }.thenBy { it.x })
        MapMessages
            .heightMapUpdates(tiles.map { TileValue(it, RoomHeights.value(room, it)) })
            .forEach { scope.out.send(Lane.Now, Audience.Room, HeightMapUpdateOutCodec, it) }
        return RollerSlides.item(furni.id, fromZ, z)
    }

    private fun shift(
        tile: Point,
        from: Point,
        to: Point,
    ): Point = Point(tile.x + to.x - from.x, tile.y + to.y - from.y)
}
