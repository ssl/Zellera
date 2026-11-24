package zel.room.furni.place

import zel.game.items.Item
import zel.room.Room
import zel.room.furni.FurniKind
import zel.room.furni.RoomFurni
import zel.room.furni.RoomHeights
import zel.room.furni.WaterPools
import zel.room.map.Footprint
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitStatus
import kotlin.math.abs

/** Pure reads of the room under its lock. */
object FloorLanding {
    const val STACK_CEILING = 80.0

    const val WIRED_CEILING = 40.0

    private const val EPSILON = 1e-4

    sealed interface Result {
        data class Lands(
            val tiles: List<Point>,
            val z: Double,
        ) : Result

        data object Refused : Result
    }

    fun place(
        room: Room,
        definition: Item,
        anchor: Point,
        direction: Int,
        unitsBlock: Boolean = false,
    ): Result {
        val tiles = Footprint.tiles(anchor, definition.width, definition.length, direction)
        if (!onFloor(room, tiles) || (unitsBlock && hasUnits(room, tiles))) return Result.Refused
        val z =
            when (FurniKind.of(definition)) {
                FurniKind.StackHelper, FurniKind.WalkTile -> {
                    heightSetterZ(room, tiles)
                }

                else -> {
                    RoomHeights.stackingHeight(room, tiles)?.takeIf {
                        specialAllows(room, definition, tiles, null) &&
                            fits(it, definition.height)
                    }
                }
            }
        return if (z == null) Result.Refused else Result.Lands(tiles, z)
    }

    /**
     * Where [furni] lands when moved or turned to [anchor]. [helperFirst] (wired moves): a stack helper under the
     * anchor also sets the z of a moved helper or walk tile, which otherwise goes to the floor.
     */
    fun move(
        room: Room,
        furni: RoomFurni,
        anchor: Point,
        direction: Int,
        unitsBlock: Boolean = false,
        helperFirst: Boolean = false,
    ): Result {
        val definition = furni.definition
        val tiles = Footprint.tiles(anchor, definition.width, definition.length, direction)
        val resting = furni.takeIf { anchor == it.anchor }
        if (!onFloor(room, tiles) || (unitsBlock && hasUnits(room, tiles, resting))) return Result.Refused
        val floor = floorAt(room, anchor)
        val helperZ = room.furni.at(anchor).helperZ(furni)
        val z =
            when {
                helperFirst && helperZ != null -> {
                    helperZ.takeIf { it >= floor && fits(it, furni.height) }
                }

                furni.kind == FurniKind.StackHelper || furni.kind == FurniKind.WalkTile -> {
                    floor
                }

                helperZ != null -> {
                    helperZ.takeIf { it >= floor && fits(it, furni.height) }
                }

                specialAllows(room, definition, tiles, furni) -> {
                    RoomHeights.stackingHeight(room, tiles, furni)?.takeIf {
                        it >= floor && fits(it, furni.height)
                    }
                }

                else -> {
                    null
                }
            }
        return if (z == null) Result.Refused else Result.Lands(tiles, z)
    }

    private fun fits(
        z: Double,
        height: Double,
    ): Boolean = z + height <= STACK_CEILING + EPSILON

    private fun onFloor(
        room: Room,
        tiles: List<Point>,
    ): Boolean = tiles.all(room.floor::isFloor)

    private fun hasUnits(
        room: Room,
        tiles: List<Point>,
        moving: RoomFurni? = null,
    ): Boolean = room.units.all().any { it.tile in tiles && (moving == null || !restsOn(room, it, moving)) }

    /**
     * [unit] sits or lies on [furni] itself: on one of its tiles, at its z, with [furni] the top item there. Such a
     * unit does not block the furni's turn in place; it turns along with it (a bed turned with its user lying on it).
     * A move to another anchor is still blocked by it.
     */
    fun restsOn(
        room: Room,
        unit: RoomUnit,
        furni: RoomFurni,
    ): Boolean {
        val seated =
            (unit.showsStatus(UnitStatus.SIT) && furni.definition.canSit) ||
                (unit.showsStatus(UnitStatus.LAY) && furni.definition.canLay)
        return seated &&
            unit.tile in furni.tiles() &&
            abs(unit.z - furni.z) < EPSILON &&
            room.furni.at(unit.tile).topItem() === furni
    }

    private fun floorAt(
        room: Room,
        tile: Point,
    ): Double = room.floor.floorZ(tile) ?: 0.0

    private fun heightSetterZ(
        room: Room,
        tiles: List<Point>,
    ): Double =
        tiles.maxOf { tile ->
            val floor = floorAt(room, tile)
            room.furni.at(tile).stackingHeight(floor) ?: floor
        }

    private fun specialAllows(
        room: Room,
        definition: Item,
        tiles: List<Point>,
        exclude: RoomFurni?,
    ): Boolean {
        val type = Interactions.of(definition)
        if (type !in Interactions.ON_EMPTY_TILES && type != Interactions.WATER) return true
        if (WaterPools.goesOnFurni(definition)) return true
        val others = tiles.flatMap { room.furni.at(it).furni }.filter { it !== exclude }
        return if (type == Interactions.WATER) {
            others.all { Interactions.of(it.definition) == Interactions.WATER }
        } else {
            others.isEmpty()
        }
    }
}
