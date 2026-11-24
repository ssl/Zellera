package zel.wired.effect.furni

import zel.game.items.FurniHeights
import zel.protocol.generated.ObjectAddOutCodec
import zel.protocol.generated.ObjectRemoveOutCodec
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.HeightMapChange
import zel.room.furni.RoomFurni
import zel.room.furni.TileStack
import zel.room.furni.place.FloorLanding
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Placeable
import zel.room.furni.place.UnitsOnTiles
import zel.room.map.Footprint
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitStatus
import zel.wired.firing.Firing
import zel.wired.firing.Movement
import zel.wired.timing.RoomPoster
import zel.wired.timing.WIRED_MOVE
import zel.wired.timing.dropWiredMoveNextTick
import zel.wired.timing.postureHeight
import kotlin.math.abs

/**
 * Wired furni moves. [view] says whether the room's viewers have a furni now (the invisible furni controller and the
 * area hides that are on keep some out): a move that takes a furni out of view removes it instead of moving it, one
 * that brings it back adds it first, and one that keeps it out sends nothing.
 */
class FurniMoves(
    private val units: UnitsOnTiles,
    private val poster: RoomPoster,
    private val view: (Room) -> (RoomFurni) -> Boolean = { { true } },
) {
    fun landing(
        room: Room,
        furni: RoomFurni,
        tile: Point,
        direction: Int = furni.direction,
        unitsBlock: Boolean = true,
    ): FloorLanding.Result.Lands? {
        if (!canTurn(furni, direction)) return null
        return landingIn(room, furni, tile, direction, unitsBlock)
    }

    private fun canTurn(
        furni: RoomFurni,
        direction: Int,
    ): Boolean = direction == furni.direction || furni.definition.allowsDirection(direction)

    private fun landingIn(
        room: Room,
        furni: RoomFurni,
        tile: Point,
        direction: Int,
        unitsBlock: Boolean,
    ): FloorLanding.Result.Lands? {
        val tiles = Footprint.tiles(tile, furni.definition.width, furni.definition.length, direction)
        if (tile == furni.anchor && furni.tiles().containsAll(tiles)) return FloorLanding.Result.Lands(tiles, furni.z)
        return (
            FloorLanding.move(
                room,
                furni,
                tile,
                direction,
                unitsBlock,
                helperFirst = true,
            ) as? FloorLanding.Result.Lands
        )?.takeIf { it.z <= FloorLanding.WIRED_CEILING }
    }

    fun move(
        firing: Firing,
        furni: RoomFurni,
        tile: Point,
        direction: Int = furni.direction,
        unitsBlock: Boolean = true,
        z: Double? = null,
        turnSlides: Boolean = false,
    ): Boolean {
        if (!canTurn(furni, direction)) return false
        val from = furni.anchor ?: return false
        val style = style(firing)
        val shape =
            addons(firing).fold(MoveShape(direction, style.duration)) { shape, addon ->
                addon.shape(firing, shape, from, tile)
            }
        val lands = addonLanding(firing, furni, tile, shape.direction, unitsBlock) ?: return false
        val toZ = z ?: if (style.keepsAltitude) furni.z else lands.z
        val turned = shape.direction != furni.direction
        if (from == tile && !turned && toZ == furni.z) return false
        val fromZ = furni.z
        val oldTiles = furni.tiles()
        val shown = view(firing.scope.room)
        val wasShown = shown(furni)
        place(firing, furni, tile, shape.direction, toZ, lands.tiles, unitsLater = true)
        firing.moved += furni.id
        val isShown = shown(furni)
        // A turn in place is always an ObjectUpdate; a move that also turns slides with the new direction when the
        // caller says so (move_rotate: one WiredMovements entry, new direction, 500 ms).
        val slidesTurned = style.turnsSlide || (turnSlides && from != tile)
        val update = (turned && !slidesTurned) || !style.animated || shape.duration == 0
        viewed(firing.scope, furni, wasShown, isShown) {
            if (update) {
                firing.flash.objectUpdate(furni.id)
            } else {
                firing.flash.movements.add(slide(furni, from, fromZ, tile, toZ, shape.duration))
            }
        }
        val move = FloorMove(furni, from, fromZ, tile, toZ)
        addons(firing).forEach { it.landed(firing, move) }
        // After the add-ons: carry users must see the users still standing on the furni, not lowered off it.
        units.furniLanded(firing.scope, oldTiles, lands.tiles)
        return true
    }

    fun lift(
        firing: Firing,
        furni: RoomFurni,
        z: Double,
    ) {
        val scope = firing.scope
        val tile = furni.anchor ?: return
        val fromZ = furni.z
        if (z == fromZ) return
        val style = style(firing)
        val riders = riders(scope.room, furni)
        val riderLines = riders.map { Rider(it, it.z + it.postureHeight) }
        riders.forEach { it.z += z - fromZ }
        place(firing, furni, tile, furni.direction, z, furni.tiles(), unitsLater = true)
        firing.moved += furni.id
        // A furni out of view stays out on its own tile: its lift sends nothing (as a move that keeps it out).
        val shown = view(scope.room)(furni)
        viewed(scope, furni, shown, shown) {
            if (style.animated) {
                firing.flash.movements.add(slide(furni, tile, fromZ, tile, z, style.duration))
            } else {
                firing.flash.objectUpdate(furni.id)
            }
        }
        riderLines.forEach { slideRider(firing, it, style) }
        // A unit higher up the same tiles goes to their new top too, a tick later.
        units.furniLanded(scope, furni.tiles(), furni.tiles())
    }

    /**
     * The ObjectUpdate and the HeightMapUpdates go out with the flash, once per furni and tile (see
     * [zel.wired.firing.FlashOut.send]).
     */
    private fun place(
        firing: Firing,
        furni: RoomFurni,
        tile: Point,
        direction: Int,
        z: Double,
        newTiles: List<Point>,
        unitsLater: Boolean = false,
    ) {
        val scope = firing.scope
        val oldTiles = furni.tiles()
        firing.flash.heights(HeightMapChange.of(scope.room, oldTiles + newTiles))
        scope.room.furni.move(furni, tile, direction)
        furni.z = z
        furni.dirty = true
        if (!unitsLater) units.furniChanged(scope, oldTiles, newTiles)
        (furni.behaviour as? Placeable)?.onMoved(scope, furni, oldTiles)
    }

    private fun slideRider(
        firing: Firing,
        rider: Rider,
        style: MoveStyle,
    ) {
        val unit = rider.unit
        val at = unit.tile
        val to = FurniHeights.formatZ(unit.z + unit.postureHeight)
        firing.flash.movements.add(
            Movement.UnitMove(
                unit.index,
                Movement.Spot(at.x, at.y, FurniHeights.formatZ(rider.fromZ)),
                Movement.Spot(at.x, at.y, to),
                Movement.UnitMove.SLIDE,
                style.duration,
                unit.bodyDirection,
                unit.headDirection,
            ),
        )
        markMoved(unit)
        poster.dropWiredMoveNextTick(firing.scope, unit)
    }

    /** The status line shows "wf" before the posture. */
    private fun markMoved(unit: RoomUnit) {
        val posture = listOf(UnitStatus.SIT, UnitStatus.LAY).mapNotNull { key -> unit.statuses[key]?.let { key to it } }
        posture.forEach { (key, _) -> unit.dropStatus(key) }
        unit.setStatus(WIRED_MOVE)
        posture.forEach { (key, value) -> unit.setStatus(key, value) }
    }

    /**
     * Units on the furni: standing at its top, or seated/lying on it (a seat puts the unit at the seat's z), a unit
     * standing inside a furni nobody can stand on, at its z (it rides along when the furni is lifted). A user riding
     * a pet on it stays where it is.
     */
    private fun riders(
        room: Room,
        furni: RoomFurni,
    ): List<RoomUnit> {
        val tiles = furni.tiles()
        val near = room.units.all().filter { it.tile in tiles && !it.isWalking }
        val onIt =
            near.filter { unit ->
                room.furni.at(unit.tile).topItem() === furni &&
                    (onFurni(unit.z, unit.postureHeight, furni.z, furni.top) || inside(unit, furni))
            }
        return onIt
    }

    private fun inside(
        unit: RoomUnit,
        furni: RoomFurni,
    ): Boolean =
        !furni.definition.canWalk &&
            !furni.definition.canSit &&
            !furni.definition.canLay &&
            abs(unit.z - furni.z) < RIDER_TOLERANCE

    /**
     * A move as the room's viewers see it, by whether they had the furni before ([wasShown]) and have it after
     * ([isShown]). Into a hidden tile: an ObjectRemove at the move and no [send]. Out of view before and after:
     * nothing at all. Back in view: ObjectAdd at the new spot, then [send] (an ObjectUpdate or the slide).
     */
    private fun viewed(
        scope: RoomScope,
        furni: RoomFurni,
        wasShown: Boolean,
        isShown: Boolean,
        send: () -> Unit,
    ) {
        if (!isShown) {
            if (wasShown) hidden(scope, furni)
            return
        }
        if (!wasShown) objectAdd(scope, furni)
        send()
    }

    private fun objectAdd(
        scope: RoomScope,
        furni: RoomFurni,
    ) = scope.out.send(Lane.AfterUnitUpdate, Audience.Room, ObjectAddOutCodec, FurniWire.objectAdd(furni))

    private fun hidden(
        scope: RoomScope,
        furni: RoomFurni,
    ) = scope.out.send(Lane.AfterUnitUpdate, Audience.Room, ObjectRemoveOutCodec, FurniWire.objectHidden(furni))

    private fun slide(
        furni: RoomFurni,
        from: Point,
        fromZ: Double,
        to: Point,
        toZ: Double,
        duration: Int,
    ) = Movement.FurniMove(
        furni.id,
        Movement.Spot(from.x, from.y, FurniHeights.formatZ(fromZ)),
        Movement.Spot(to.x, to.y, FurniHeights.formatZ(toZ)),
        furni.direction,
        duration,
    )

    fun style(firing: Firing): MoveStyle = styleOf(firing)

    fun allowedLanding(
        firing: Firing,
        furni: RoomFurni,
        tile: Point,
        direction: Int = furni.direction,
        unitsBlock: Boolean = true,
    ): FloorLanding.Result.Lands? =
        if (canTurn(furni, direction)) addonLanding(firing, furni, tile, direction, unitsBlock) else null

    private fun addonLanding(
        firing: Firing,
        furni: RoomFurni,
        tile: Point,
        direction: Int,
        unitsBlock: Boolean,
    ): FloorLanding.Result.Lands? {
        val through = addons(firing).firstNotNullOfOrNull { it.through(firing) }
        val room = firing.scope.room
        val lands =
            if (through == null) {
                landingIn(room, furni, tile, direction, unitsBlock)
            } else {
                landingThrough(room, furni, tile, direction, unitsBlock, through)
            } ?: return null
        return lands.takeUnless { addons(firing).any { addon -> addon.refuses(firing, furni, lands.tiles) } }
    }

    private fun landingThrough(
        room: Room,
        furni: RoomFurni,
        tile: Point,
        direction: Int,
        unitsBlock: Boolean,
        through: MoveThrough,
    ): FloorLanding.Result.Lands? {
        val tiles = Footprint.tiles(tile, furni.definition.width, furni.definition.length, direction)
        val blocked =
            unitsBlock &&
                room.units.all().any {
                    it.tile in tiles && it.index !in through.unitIndexes &&
                        !(tile == furni.anchor && FloorLanding.restsOn(room, it, furni))
                }
        if (blocked || !tiles.all(room.floor::isFloor)) return null
        val z =
            tiles.maxOf { spot ->
                val floor = room.floor.floorZ(spot) ?: return null
                val rest =
                    room.furni
                        .at(spot)
                        .furni
                        .filter { it !== furni && it.id !in through.furniIds }
                TileStack(rest).stackingHeight(floor) ?: return null
            }
        return FloorLanding.Result.Lands(tiles, z).takeIf { z <= FloorLanding.WIRED_CEILING }
    }

    private fun addons(firing: Firing): List<MoveAddon> = firing.addons.filterIsInstance<MoveAddon>()

    private class Rider(
        val unit: RoomUnit,
        val fromZ: Double,
    )

    companion object {
        fun styleOf(firing: Firing): MoveStyle =
            firing.addons.filterIsInstance<MoveAddon>().fold(MoveStyle()) { style, addon -> addon.style(firing, style) }

        const val SLIDE_MILLIS = 500

        /** How close a unit's z must be to count as on a furni. */
        private const val RIDER_TOLERANCE = 0.01

        /** A unit at [z] with [postureHeight] is on a furni from [furniZ] to [top]: standing at its top, or seated. */
        fun onFurni(
            z: Double,
            postureHeight: Double,
            furniZ: Double,
            top: Double,
        ): Boolean = abs(z - top) < RIDER_TOLERANCE || (postureHeight > 0.0 && abs(z - furniZ) < RIDER_TOLERANCE)
    }
}
